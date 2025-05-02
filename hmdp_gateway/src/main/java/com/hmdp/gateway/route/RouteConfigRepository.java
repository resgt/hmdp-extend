package com.hmdp.gateway.route;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hmdp.gateway.mapper.RouteMapper;

import com.hmdp.gateway.util.RouteConvert;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Log4j2
@Repository
public class RouteConfigRepository implements RouteDefinitionRepository {

    @Autowired
    private RouteMapper routeMapper;

    // gateway默认每30秒去数据库获取最新的路由列表
    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        List<RouteConfig> routeEntityList = routeMapper.selectList(Wrappers.emptyWrapper());
        if (CollUtil.isEmpty(routeEntityList)) {
            log.info("数据库内没有定义路由");
            return Flux.empty();
        }
        List<RouteDefinition> routeDefinitionList = new ArrayList<>();
        routeEntityList.stream().forEach(routeEntity -> {
            routeDefinitionList.add(RouteConvert.toRouteDefinition(routeEntity));
        });
        log.info("json 数据库路由列表: {}", JSONUtil.toJsonStr(routeDefinitionList));
        return Flux.fromIterable(routeDefinitionList);
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return null;
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return null;
    }

}



