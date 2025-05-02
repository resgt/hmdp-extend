package com.hmdp.common.redislock.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import com.hmdp.common.redislock.ILock;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.PathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁 的 进阶实现 （不如redisson但是够用）
 * 缺点：不可重入
 */
public class AdvancedRedisLock implements ILock {

    private StringRedisTemplate stringRedisTemplate;

    // 锁的名字
    private String name;

    // 锁的前缀
    private static final String KEY_PREFIX = "lock:";

    // 线程标识的前缀
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    // lua脚本
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    // 初始化lua脚本
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("lua\\unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public AdvancedRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryGetLock(Long timeoutSec) {
        // 获取当前线程id
        long threadId = Thread.currentThread().getId();
        // 拼接成线程标识
        String threadFlag = ID_PREFIX + threadId;
        // 获取锁
        Boolean isFlag = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadFlag, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(isFlag);
    }

    @Override
    public void unLock() {
        // 获取线程标识
        String threadFlag = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁的key
        String key = KEY_PREFIX + name;
        // 调用lua脚本
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), threadFlag);
    }
}
