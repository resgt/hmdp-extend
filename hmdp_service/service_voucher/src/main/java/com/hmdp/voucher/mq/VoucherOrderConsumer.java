package com.hmdp.voucher.mq;

import com.hmdp.model.entity.VoucherOrder;
import com.hmdp.voucher.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = "voucher-order-topic",
        consumerGroup = "${rocketmq.consumer.group}",
        // 指定最大重试次数
        maxReconsumeTimes = 3
)
public class VoucherOrderConsumer implements RocketMQListener<VoucherOrderMessage> {

    @Autowired
    private IVoucherOrderService voucherOrderService;
    
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void onMessage(VoucherOrderMessage message) {
        log.info("收到优惠券下单消息，订单id：{}", message.getOrderId());
        // 获取用户id
        Long userId = message.getUserId();
        // 创建锁对象
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 获取锁
        boolean isLock = lock.tryLock();
        // 判断是否获取锁成功
        if (!isLock) {
            log.error("不允许重复下单");
            // 稍后重试
            throw new RuntimeException("获取锁失败，等待重试");
        }
        try {
            // 创建订单
            createVoucherOrder(message);
        } catch (Exception e) {
            log.error("订单创建失败，将进行重试，订单id：{}，异常：{}", message.getOrderId(), e.getMessage());
            // 抛出异常触发重试机制
            throw new RuntimeException("订单创建失败，等待重试", e);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }
    
    private void createVoucherOrder(VoucherOrderMessage message) {
        try {
            // 查询订单是否已存在
            VoucherOrder existOrder = voucherOrderService.getById(message.getOrderId());
            if (existOrder != null) {
                log.info("订单已存在，无需重复创建，订单id：{}", message.getOrderId());
                return;
            }
            
            // 创建订单
            VoucherOrder voucherOrder = new VoucherOrder();
            voucherOrder.setId(message.getOrderId());
            voucherOrder.setUserId(message.getUserId());
            voucherOrder.setVoucherId(message.getVoucherId());
            voucherOrder.setStatus(1); // 未支付状态
            // 设置创建时间和更新时间
            voucherOrder.setCreateTime(LocalDateTime.now());
            voucherOrder.setUpdateTime(LocalDateTime.now());
            voucherOrderService.createVoucherOrderWithLua(voucherOrder);
            log.info("订单创建成功，订单id：{}", message.getOrderId());
        } catch (Exception e) {
            log.error("创建订单异常，订单id：{}，异常：{}", message.getOrderId(), e.getMessage());
            throw e;
        }
    }
}
