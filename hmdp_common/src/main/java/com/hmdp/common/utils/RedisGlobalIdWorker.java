package com.hmdp.common.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * redis全局唯一id生成器
 */
public class RedisGlobalIdWorker {

    // 定义起始时间的秒数
    private static final long BEGIN_TIMESTAMP = 1640995200L;

    // 序列号的位数
    private static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisGlobalIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 生成id
     * @param keyPrefix redis 的 key 前缀
     * @return
     */
    public long nextId(String keyPrefix) {
        // 获取当前时间的秒数
        long nowSecond = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        // 生成时间戳
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        // 获取当前日期，精确到天
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 生成序列号，以天为单位生成序列号，防止同一个key生成的序列号溢出
        long count = stringRedisTemplate.opsForValue().increment("ice:" + keyPrefix + ":" + date);
        // 拼接并返回（拼接原理：时间戳先向左移动COUNT_BITS位，再引用 或 运算将序列号拼接到低位）
        return timestamp << COUNT_BITS | count;
    }


}
