package com.hmdp.common.redislock.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.common.redislock.ILock;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁 的 简单实现
 * 缺点： 存在 锁误删 问题
 */
public class SimpleRedisLock implements ILock {

    private StringRedisTemplate stringRedisTemplate;

    // 锁的名字
    private String name;

    // 锁的前缀
    private static final String KEY_PREFIX = "lock:";

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryGetLock(Long timeoutSec) {
        // 获取当前线程id
        long threadId = Thread.currentThread().getId();
        // 获取锁
        Boolean isFlag = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, JSONUtil.toJsonStr(threadId), timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(isFlag);
    }

    @Override
    public void unLock() {
        stringRedisTemplate.delete(KEY_PREFIX + name);
    }
}
