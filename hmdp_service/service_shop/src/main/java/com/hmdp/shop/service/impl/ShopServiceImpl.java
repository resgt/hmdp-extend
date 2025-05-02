package com.hmdp.shop.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.common.utils.RedisConstants;
import com.hmdp.common.utils.RedisData;
import com.hmdp.common.utils.SystemConstants;
import com.hmdp.model.dto.Result;
import com.hmdp.model.entity.Shop;
import com.hmdp.shop.mapper.ShopMapper;
import com.hmdp.shop.service.IShopService;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.common.utils.RedisConstants.*;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    // 可重用固定线程数的线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        // 方案一：单纯解决缓存穿透问题
//        Shop shop = this.getByIdWithPassThrough(id);

        // 方案二：解决缓存击穿问题和利用互斥锁解决缓存击穿问题
        Shop shop = getByIdWithMutex(id);

        // 方案三：利用逻辑过期解决缓存击穿问题（该方案需要后台管理员先进行数据预热）
//        Shop shop = this.getByIdWithLogicalExpire(id);

        if (shop == null) return Result.fail("店铺不存在");
        // 返回数据
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        // 判断商铺id是否存在
        Long shopId = shop.getId();
        if (shopId == null) return Result.fail("店铺id不能为空");
        // 更新数据库
        this.updateById(shop);
        // 删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shopId);
        return null;
    }

    // 获取锁，基于redis的setnx命令实现
    private boolean tryGetLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        // 利用工具类做拆箱
        boolean isTrue = BooleanUtil.isTrue(flag);
        return isTrue;
    }

    // 释放锁，基于redis的setnx命令实现
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

    // 根据id获取商铺信息并且 防止缓存穿透
    private Shop getByIdWithPassThrough(Long id) {
        // 从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        /* 判断是否存在
         * StrUtil.isNotBlank(null) return false;
         * StrUtil.isNotBlank("") return false;
         * StrUtil.isNotBlank("\t\n") return false;
         * StrUtil.isNotBlank("abc") return true;
         * */
        if (StrUtil.isNotBlank(shopJson)) {
            // 存在，转换为shop对象并且返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        // 判断是否命中的是空字符串，空字符串则代表不需要再去访问数据库了， 防止缓存穿透
        if (shopJson != null) return null;
        // 不存在，根据id查询数据库
        Shop shop = this.getById(id);
        // 判断数据中是否存在
        if (shop == null) {
            // 将空值写入redis，防止缓存穿透
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 存入redis中并设置有效时间
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 返回数据
        return shop;
    }

    // 根据id获取商铺信息并且 防止缓存穿透 和 利用互斥锁防止缓存击穿
    private Shop getByIdWithMutex(Long id) {
        // 从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        /* 判断是否存在
         * StrUtil.isNotBlank(null) return false;
         * StrUtil.isNotBlank("") return false;
         * StrUtil.isNotBlank("\t\n") return false;
         * StrUtil.isNotBlank("abc") return true;
         * */
        if (StrUtil.isNotBlank(shopJson)) {
            // 存在，转换为shop对象并且返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        // 判断是否命中的是空字符串，空字符串则代表不需要再去访问数据库了， 防止缓存穿透
        if (shopJson != null) return null;
        Shop shop = null;
        try {
            // 实现缓存重建，防止缓存击穿
            // 获取互斥锁
            boolean isGetLock = this.tryGetLock(LOCK_SHOP_KEY + id);
            // 判断是否获取成功
            if (!isGetLock) {
                // 获取锁失败，休眠
                Thread.sleep(50);
                // 重新获取锁
                return getByIdWithMutex(id);
            }
            // 获取锁成功，则根据id查询数据库
            shop = this.getById(id);
            // 判断数据中是否存在
            if (shop == null) {
                // 将空值写入redis，防止缓存穿透
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 存入redis中并设置有效时间
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 释放互斥锁
            this.unLock(LOCK_SHOP_KEY + id);
        }
        // 返回数据
        return shop;
    }

    // 模拟后台管理员端根据id获取店铺信息放入redis(逻辑过期方案的提前预热)
    public void saveShopToRedis(Long id, Long expireSeconds) {
        // 查询店铺信息
        Shop shop = this.getById(id);
        // 将shop封装进RedisData
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    // 根据id获取商铺信息并且 利用逻辑过期防止缓存击穿 （不需要考虑缓存穿透问题，因为空值直接返回，访问不到数据库）
    private Shop getByIdWithLogicalExpire(Long id) {
        // 从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        /* 判断是否存在
         * StrUtil.isNotBlank(null) return false;
         * StrUtil.isNotBlank("") return false;
         * StrUtil.isNotBlank("\t\n") return false;
         * StrUtil.isNotBlank("abc") return true;
         * */
        if (StrUtil.isBlank(shopJson)) {
            // 不存在，直接放回null，因为redis中已经进行了数据的提前预热，没有预热的数据证明没有，直接返回空，不需要去查询数据库
            return null;
        }
        // 存在，需要判断过期时间
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        // 获取具体店铺数据
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        // 获取逻辑过期时间
        LocalDateTime expireTime = redisData.getExpireTime();
        // 判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) return shop; // 未过期，直接返回店铺数据
        // 已过期，进行缓存重建
        // 获取互斥锁
        boolean isLock = this.tryGetLock(LOCK_SHOP_KEY + id);
        // 判断是否获取锁成功
        if (isLock) {
            // 成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 重建缓存
                    this.saveShopToRedis(id, 1800L);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // 释放锁
                    this.unLock(LOCK_SHOP_KEY + id);
                }
            });
        }
        // 返回数据
        return shop;
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 判断是否需要坐标查询
        if (x == null || y == null) {
            // 不需要，查询数据库
            Page<Shop> page = this.query().eq("type_id", typeId).page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }
        String key = SHOP_GEO_KEY + typeId;
        // 分页开始位置
        long from = (long) (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        // 分页结束位置
        long end = (long) current * SystemConstants.DEFAULT_PAGE_SIZE;
        // 够找geo查询条件
        Circle circle = new Circle(new Point(x, y), // 圆心经纬度
                new Distance(5000, RedisGeoCommands.DistanceUnit.METERS) // 半径
        );
        // 查询redis，按照距离排序、分页
        GeoResults<RedisGeoCommands.GeoLocation<String>> radius = stringRedisTemplate.opsForGeo().radius(
                key,
                circle,
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().limit(end));
        // 判断查询结果是否为空
        if (radius == null) return Result.ok(Collections.emptyList());
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = radius.getContent();
        // 判断是否还有下一页数据
        if (list.size() <= from) return Result.ok(Collections.emptyList());
        // 店铺id收集
        List<Long> ids = new ArrayList<>(list.size());
        // 距离收集
        Map<Long, Distance> distanceMap = new HashMap<>(list.size());
        // 截取 from -> end 的部分
        list.stream().skip(from).forEach(radiu -> {
            // 获取店铺id
            Long shopId = Long.valueOf(radiu.getContent().getName());
            ids.add(shopId);
            // 获取距离
            Distance distance = radiu.getDistance();
            distanceMap.put(shopId, distance);
        });
        // 根据id查询shop
        List<Shop> shopList =baseMapper.getShopListWithOrder(ids);
        for (Shop shop : shopList) {
            shop.setDistance(distanceMap.get(shop.getId()).getValue());
        }
        return Result.ok(shopList);
    }
}
