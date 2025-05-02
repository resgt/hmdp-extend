package com.hmdp.voucher.mq;

import com.hmdp.model.entity.VoucherOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VoucherOrderProducer {
    
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    
    private static final String TOPIC = "voucher-order-topic";
    
    /**
     * 发送优惠券下单消息
     * @param orderId 订单id
     * @param userId 用户id
     * @param voucherId 优惠券id
     */
    public void sendOrderMessage(Long orderId, Long userId, Long voucherId) {
        try {
            VoucherOrderMessage message = new VoucherOrderMessage(orderId, userId, voucherId);
            rocketMQTemplate.convertAndSend(TOPIC, message);
            log.info("优惠券下单消息发送成功，订单id：{}", orderId);
        } catch (Exception e) {
            log.error("优惠券下单消息发送失败，订单id：{}", orderId, e);
            throw new RuntimeException("消息发送失败", e);
        }
    }
}
