package com.hmdp.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        // 创建配置对象
        Config config = new Config();
        // 设置单点的地址，也可以使用config.useClusterServers()设置集群地址
        config.useSingleServer().setAddress("redis://172.27.52.12:6379");
        // 创建RedissonClient对象并返回
        return Redisson.create(config);
    }

}
