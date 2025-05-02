package com.hmdp.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 全局过滤器
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    //路径匹配器，支持通配符（用于比较url的）
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // 不需要过滤的的请求路径
    private static final String[] NO_FILTER_URL = {
            "/shop/**",
            "/voucher/**",
            "/shop-type/**",
            "/upload/**",
            "/blog/hot",
            "/user/code",
            "/user/login"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求
        ServerHttpRequest request = exchange.getRequest();
        // 获取请求路径
        String requestURI = request.getURI().getPath();
        // 判断该请求是否需要拦截
        boolean check = check(NO_FILTER_URL, requestURI);
        System.out.println("拦截到请求：" + requestURI);
        /* 获取token ,一般和前端约定好 一般把token放在请求头里面
                一般 key为authorization（授权的意思）
                    value为token
         */
        String token = request.getHeaders().getFirst("authorization");
        if (check) {
            log.info("不需要拦截，请求路径为：{}", requestURI);
            // 虽然不需要拦截，但是也得刷新一下redis缓存，不然时间一到会自动清空缓存
            stringRedisTemplate.expire("login:token:" + token, 60L, TimeUnit.MINUTES);
            return chain.filter(exchange);
        }
        // 判断集合是否为空
        if (!StringUtils.isEmpty(token)) {
            // 判断token是否为空字符串
            if (StringUtils.hasText(token)) {
                // 不为空字符串
                Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries("login:token:" + token);
                // 判断用户是否存在
                if (userMap.isEmpty()) {
                    // 不存在，拦截并返回状态码
                    return getVoidMono(exchange);
                }
                log.info("可以放行， 请求为：{}", requestURI);
                // 存在则刷新token有效期并且放行
                stringRedisTemplate.expire("login:token:" + token, 60L, TimeUnit.MINUTES);
                return chain.filter(exchange);
            }
        }
        // 集合为空，拦截并返回状态码
        return getVoidMono(exchange);
    }

    /**
     * 拦截并返回状态码
     * @param exchange
     * @return
     */
    private Mono<Void> getVoidMono(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        DataBuffer buffer = response.bufferFactory().wrap("未登录".getBytes(StandardCharsets.UTF_8));
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 路径匹配，检查本次请求是否需要放行
     * @param urls
     * @param requestURI
     * @return 放行：true  拦截：false
     */
    private static boolean check(String[] urls,String requestURI){
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if(match){
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
