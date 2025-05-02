package com.hmdp.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.model.entity.ShopType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShopTypeMapper extends BaseMapper<ShopType> {

    // 查询全部并且按照sort排序
    List<ShopType> selectListAndOrder();
}
