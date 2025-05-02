package com.hmdp.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.model.entity.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShopMapper extends BaseMapper<Shop> {

    List<Shop> getShopListWithOrder(List<Long> ids);

    @Select("SELECT *, " +
            "ST_Distance_Sphere(point(longitude, latitude), point(#{x}, #{y})) AS distance " +
            "FROM tb_shop " +
            "WHERE (name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR area LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR address LIKE CONCAT('%', #{keyword}, '%')) " +
            "HAVING distance <= 5000 " +  // 5公里内
            "ORDER BY distance " +
            "LIMIT #{offset}, #{size}")
    List<Shop> findNearbyShops(@Param("x") Double x, 
                              @Param("y") Double y,
                              @Param("keyword") String keyword,
                              @Param("offset") Integer offset,
                              @Param("size") Integer size);

    @Select("SELECT COUNT(*) " +
            "FROM tb_shop " +
            "WHERE (name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR area LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR address LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND ST_Distance_Sphere(point(longitude, latitude), point(#{x}, #{y})) <= 5000")
    long countNearbyShops(@Param("x") Double x,
                         @Param("y") Double y,
                         @Param("keyword") String keyword);
}
