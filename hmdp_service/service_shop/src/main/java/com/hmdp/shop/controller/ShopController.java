package com.hmdp.shop.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.common.utils.SystemConstants;
import com.hmdp.model.dto.Result;
import com.hmdp.model.entity.Shop;
import com.hmdp.shop.service.IShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shop")
public class ShopController {

    @Autowired
    private IShopService shopService;

    /**
     * 根据id查询商铺信息
     * @param id 商铺id
     * @return 商品详情数据
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        return shopService.queryById(id);
    }

    /**
     * 更新商铺信息
     * @param shop  商铺数据
     * @return
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        // 更新数据库并删除redis缓存
        return shopService.update(shop);
    }

    /**
     * 根据商铺类型分页查询商铺信息
     * @param typeId 商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y
    ) {
        return shopService.queryShopByType(typeId, current, x, y);
    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     * @param name 商铺名称关键字
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
//        Page<Shop> page = shopService.query()
//                .like(StrUtil.isNotBlank(name), "name", name)
//                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        Page<Shop> page = new Page<>(current, SystemConstants.MAX_PAGE_SIZE);
        shopService.page(page, new LambdaQueryWrapper<Shop>().like(StrUtil.isNotBlank(name), Shop::getName, name));
        // 返回数据
        return Result.ok(page.getRecords());
    }
}
