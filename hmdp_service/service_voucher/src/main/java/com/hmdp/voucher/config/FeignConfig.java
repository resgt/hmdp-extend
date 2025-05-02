package com.hmdp.voucher.config;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    // 设置请求超时时间
    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(6000, 6000);
    }
}