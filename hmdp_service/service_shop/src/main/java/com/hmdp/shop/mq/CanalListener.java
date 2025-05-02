package com.hmdp.shop.mq;

import com.alibaba.fastjson.JSON;
import com.hmdp.common.utils.RedisConstants;
import com.hmdp.model.entity.Shop;
import com.hmdp.shop.service.IShopService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

import static com.hmdp.common.utils.RedisConstants.CACHE_SHOP_KEY;

@Slf4j
@Component
@RocketMQMessageListener(topic = "canal.shop", consumerGroup = "shop-service-group")
public class CanalListener implements RocketMQListener<String> {

    @Resource
    private IShopService shopService;
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void onMessage(String message) {
        try {
            // 1.解析消息
            Map<String, Object> map = JSON.parseObject(message, Map.class);
            // 2.获取消息类型
            String type = map.get("type").toString();
            // 3.获取店铺数据
            Shop shop = JSON.parseObject(map.get("data").toString(), Shop.class);
            // 4.处理数据
            switch (type) {
                case "INSERT":
                case "UPDATE":
                    // 更新数据库
                    shopService.updateById(shop);
                    // 删除缓存
                    stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
                    log.info("Shop data updated and cache cleared, shopId: {}", shop.getId());
                    break;
                case "DELETE":
                    // 删除数据
                    shopService.removeById(shop.getId());
                    // 删除缓存
                    stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
                    log.info("Shop data and cache deleted, shopId: {}", shop.getId());
                    break;
                default:
                    log.warn("Unknown operation type: {}", type);
                    break;
            }
        } catch (Exception e) {
            log.error("处理Canal消息异常", e);
        }
    }
}
