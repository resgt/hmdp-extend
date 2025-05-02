package com.hmdp.gateway.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.gateway.route.RouteConfig;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface RouteMapper extends BaseMapper<RouteConfig> {

}
