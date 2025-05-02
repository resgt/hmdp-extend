package com.hmdp.shop.controller;

import com.hmdp.model.dto.Result;
import com.hmdp.model.entity.ShopType;
import com.hmdp.shop.service.IShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {

    @Autowired
    private IShopTypeService shopTypeService;

    /**
     * 查询店铺类型并且放入缓存
     * @return 店铺类型的集合
     */
    @GetMapping("/list")
    public Result queryTypeList() {
        List<ShopType> shopTypes = shopTypeService.typeList();
        if (shopTypes == null) return Result.fail("店铺类型数据不存在");
        return Result.ok(shopTypes);
    }
}
