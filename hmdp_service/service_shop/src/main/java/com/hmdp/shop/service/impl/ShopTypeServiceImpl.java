package com.hmdp.shop.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.model.dto.Result;
import com.hmdp.model.entity.ShopType;
import com.hmdp.shop.mapper.ShopTypeMapper;
import com.hmdp.shop.service.IShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    private static final String SHOP_TYPE_LIST_KEY = "shopTypeList";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> typeList() {
        // 查询redis中是否存在数据
        List<String> shopTypeList = stringRedisTemplate.opsForList().range(SHOP_TYPE_LIST_KEY, 0, -1);
        // 判断shopTypeList是否存在
        if (!CollectionUtils.isEmpty(shopTypeList)) {
            // 存在则转为对象集合在返回
            List<ShopType> shopTypes = new ArrayList<>();
            for (String shopTypeString : shopTypeList) {
                ShopType shopType = JSONUtil.toBean(shopTypeString, ShopType.class);
                shopTypes.add(shopType);
            }
            return shopTypes;
        }
        // 不存在则去数据库中查询
        List<ShopType> shopTypes = baseMapper.selectListAndOrder();
        // 判断数据是否存在
        if (CollectionUtils.isEmpty(shopTypes)) {
            // 数据不存在返回null
            return null;
        }
        // 存在则放入redis中缓存
        List<String> shopTypeStrings = new ArrayList<>();
        for (ShopType shopType : shopTypes) {
            String shopTypeString = JSONUtil.toJsonStr(shopType);
            shopTypeStrings.add(shopTypeString);
        }
        stringRedisTemplate.opsForList().leftPushAll(SHOP_TYPE_LIST_KEY, shopTypeStrings);
        // 返回数据
        return shopTypes;
    }
}
