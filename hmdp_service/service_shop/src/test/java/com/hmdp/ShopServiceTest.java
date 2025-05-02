package com.hmdp;

import cn.hutool.crypto.SecureUtil;
import com.hmdp.common.utils.RedisConstants;
import com.hmdp.common.utils.RedisGlobalIdWorker;
import com.hmdp.model.entity.Shop;
import com.hmdp.shop.ShopApplication;
import com.hmdp.shop.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@SpringBootTest(classes = ShopApplication.class)
public class ShopServiceTest {

    @Autowired
    private ShopServiceImpl shopService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 模拟管理员往redis中预热店铺信息数据
    @Test
    public void testSaveShopToRedis() {
        System.err.println("店铺数据预热开始");
        for (Long i = 1L; i < 15L; i++) {
            System.err.println("预热id为" + i + "的店铺数据成功");
            shopService.saveShopToRedis(i, 1800L);
        }
        System.err.println("店铺数据预热结束");
    }

    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    public void testRedisGlobalIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = new RedisGlobalIdWorker(stringRedisTemplate).nextId("order");
                System.err.println("id = " + id);
            }
            latch.countDown();
        };
        long beginTime = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long endTime = System.currentTimeMillis();
        System.err.println("time = " + (endTime - beginTime));
    }

    // 导入店铺数据到GEO
    @Test
    public void loadShopDataToGeo() {
        // 查询店铺信息
        List<Shop> list = shopService.list();
        // 将数据按照店铺类型进行分组
        Map<Long, List<Shop>> shopTypeList = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        // 分批写入redis
        for (Map.Entry<Long, List<Shop>> entry : shopTypeList.entrySet()) {
            Long typeId = entry.getKey();
            String key = RedisConstants.SHOP_GEO_KEY + typeId;
            List<Shop> shops = entry.getValue();

            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(shops.size());
            for (Shop shop : shops) {
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY()))
                );
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }

}
