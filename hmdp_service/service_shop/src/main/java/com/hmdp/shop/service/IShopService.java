package com.hmdp.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.model.dto.Result;
import com.hmdp.model.entity.Shop;

public interface IShopService extends IService<Shop> {
    Result queryById(Long id);

    Result update(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
