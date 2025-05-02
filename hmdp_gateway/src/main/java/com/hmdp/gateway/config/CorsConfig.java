package com.hmdp.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 全局处理跨域问题
 */
@Configuration
public class CorsConfig {
    @Bean
    public CorsWebFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许提交请求的方法，*表示全部允许，一般OPTIONS,GET,POST三个够了
        config.addAllowedMethod("*");
        // 允许向该服务器提交请求的URI，*表示全部允许，自定义可以添加多个，在SpringMVC中，如果设成*，会自动转成当前请求头中的Origin
        config.addAllowedOrigin("*");
        // 允许访问的头信息,*表示全部，可以添加多个
        config.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(new PathPatternParser());
        //对所有接口都有效
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }

}
