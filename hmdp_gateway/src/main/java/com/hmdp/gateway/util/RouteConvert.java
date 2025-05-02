package com.hmdp.gateway.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.hmdp.gateway.route.RouteConfig;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.net.URI;
import java.util.Map;

public class RouteConvert {
    private RouteConvert(){
    }

    /**
     * 转换为 RouteDefinition
     *
     * @param routeEntity
     * @return
     */
    public static RouteDefinition toRouteDefinition(RouteConfig routeEntity) {
        RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setId(routeEntity.getRouteId());
//        routeDefinition.setOrder(routeEntity.getFilterOrder());
        routeDefinition.setUri(URI.create(routeEntity.getUri()));
        JSONArray predicateArray = new JSONArray(JSONUtil.toJsonStr(routeEntity.getPredicates()));
        if (!predicateArray.isEmpty()) {
            routeDefinition.setPredicates(predicateArray.toList(PredicateDefinition.class));
        }
//        JSONArray filtersArray = new JSONArray(JSONUtil.toJsonStr(routeEntity.getFilters()));
//        if (!filtersArray.isEmpty()) {
//            routeDefinition.setFilters(filtersArray.toList(FilterDefinition.class));
//        }
//        routeDefinition.setMetadata(JSONUtil.toBean(routeEntity.getMetadata(), Map.class));
        return routeDefinition;
    }

    /**
     * 转换为路由数据库实体类
     *
     * @param routeDefinition
     * @return
     */
    public static RouteConfig toRouteEntity(RouteDefinition routeDefinition) {
        RouteConfig routeEntity = new RouteConfig();
        routeEntity.setRouteId(routeDefinition.getId());
        routeEntity.setUri(routeDefinition.getUri().toString());
        routeEntity.setPredicates(JSONUtil.toJsonStr(routeDefinition.getPredicates()));
        routeEntity.setFilters(JSONUtil.toJsonStr(routeDefinition.getFilters()));
        routeEntity.setMetadata(JSONUtil.toJsonStr(routeDefinition.getMetadata()));
        routeEntity.setFilterOrder(routeDefinition.getOrder());
        return routeEntity;
    }

}
