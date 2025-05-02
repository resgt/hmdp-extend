package com.hmdp.voucher;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAspectJAutoProxy(exposeProxy = true) // 暴露代理对象
@SpringBootApplication
@EnableDiscoveryClient
@EnableTransactionManagement
@ComponentScan(basePackages = "com.hmdp")
@MapperScan("com.hmdp.voucher.mapper")
@EnableDubbo // 启用Dubbo
public class VoucherApplication {
    public static void main(String[] args) {
        SpringApplication.run(VoucherApplication.class, args);
    }
}
