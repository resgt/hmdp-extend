package com.hmdp.follow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.hmdp.userfeign"})
public class FollowApplication {
    public static void main(String[] args) {
        SpringApplication.run(FollowApplication.class, args);
    }
}
