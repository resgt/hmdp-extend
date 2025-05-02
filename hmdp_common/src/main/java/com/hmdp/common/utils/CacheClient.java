package com.hmdp.common.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.common.utils.RedisConstants.*;

/**
 * 进一步对redisTemplate进行封装
 */
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    // 可重用固定线程数的线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 将java对象写入redis中
     * @param key redis的 key
     * @param value 需要写入redis的数据
     * @param time 过期时间
     * @param unit 过期时间类型
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 将java对象写入redis中，并且完成逻辑过期处理
     * @param key redis的 key
     * @param value 需要写入redis的数据
     * @param time 过期时间
     * @param unit 过期时间类型
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 封装数据
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 根据id获取数据信息并且 防止缓存穿透
     * @param KEY_PREFIX redis的 key 的前缀
     * @param id 数据对应的 id
     * @param type 返回哪种数据类型
     * @param dbFallback 数据库对应的操作
     * @param time 过期时间
     * @param unit 过期时间类型
     * @param <R> 返回值类型
     * @param <ID> id类型
     * @return
     */
    private <R, ID> R getByIdWithPassThrough(String KEY_PREFIX, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        // 从redis中查询数据缓存
        String valueJson = stringRedisTemplate.opsForValue().get(KEY_PREFIX + id);
        /* 判断是否存在
         * StrUtil.isNotBlank(null) return false;
         * StrUtil.isNotBlank("") return false;
         * StrUtil.isNotBlank("\t\n") return false;
         * StrUtil.isNotBlank("abc") return true;
         * */
        if (StrUtil.isNotBlank(valueJson)) {
            // 存在，转换为type对象并且返回
            return JSONUtil.toBean(valueJson, type);
        }
        // 判断是否命中的是空字符串，空字符串则代表不需要再去访问数据库了， 防止缓存穿透
        if (valueJson != null) return null;
        // 不存在，根据id查询数据库
        R apply = dbFallback.apply(id);
        // 判断数据中是否存在
        if (apply == null) {
            // 将空值写入redis，防止缓存穿透
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 存入redis中并设置有效时间
        this.set(KEY_PREFIX + id, apply, time, unit);
        // 返回数据
        return apply;
    }

    /**
     * 根据id获取数据信息并且 利用逻辑过期防止缓存击穿
     * （不需要考虑缓存穿透问题，因为空值直接返回，访问不到数据库）
     * @param KEY_PREFIX redis的 key 的前缀
     * @param LOCK_KEY 锁的前缀
     * @param id 数据对应的 id
     * @param type 返回哪种数据类型
     * @param dbFallback 数据库对应的操作
     * @param time 过期时间
     * @param unit 过期时间类型
     * @param <R> 返回值类型
     * @param <ID> id类型
     * @return
     */
    private <R, ID> R getByIdWithLogicalExpire(String KEY_PREFIX, String LOCK_KEY, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        // 从redis中查询数据缓存
        String valueJson = stringRedisTemplate.opsForValue().get(KEY_PREFIX + id);
        /* 判断是否存在
         * StrUtil.isNotBlank(null) return false;
         * StrUtil.isNotBlank("") return false;
         * StrUtil.isNotBlank("\t\n") return false;
         * StrUtil.isNotBlank("abc") return true;
         * */
        if (StrUtil.isBlank(valueJson)) {
            // 不存在，直接放回null，因为redis中已经进行了数据的提前预热，没有预热的数据证明没有，直接返回空，不需要去查询数据库
            return null;
        }
        // 存在，需要判断过期时间
        RedisData redisData = JSONUtil.toBean(valueJson, RedisData.class);
        // 获取具体数据
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        // 获取逻辑过期时间
        LocalDateTime expireTime = redisData.getExpireTime();
        // 判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) return r; // 未过期，直接返回数据
        // 已过期，进行缓存重建
        // 获取互斥锁
        boolean isLock = this.tryGetLock(LOCK_KEY+ id);
        // 判断是否获取锁成功
        if (isLock) {
            // 成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 重建缓存
                    R apply = dbFallback.apply(id);
                    this.setWithLogicalExpire(KEY_PREFIX + id, apply, time, unit);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // 释放锁
                    this.unLock(LOCK_KEY + id);
                }
            });
        }
        // 返回数据
        return r;
    }

    // 获取锁，基于redis的setnx命令实现
    private boolean tryGetLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10L, TimeUnit.SECONDS);
        // 利用工具类做拆箱
        boolean isTrue = BooleanUtil.isTrue(flag);
        return isTrue;
    }

    // 释放锁，基于redis的setnx命令实现
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
