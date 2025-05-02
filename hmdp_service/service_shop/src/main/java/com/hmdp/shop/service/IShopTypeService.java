package com.hmdp.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.model.entity.ShopType;

import java.util.List;

public interface IShopTypeService extends IService<ShopType> {
    List<ShopType> typeList();
}
