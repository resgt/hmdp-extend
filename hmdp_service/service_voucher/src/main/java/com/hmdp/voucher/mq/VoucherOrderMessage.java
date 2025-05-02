package com.hmdp.voucher.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherOrderMessage {
    /**
     * 订单id
     */
    private Long orderId;
    
    /**
     * 用户id
     */
    private Long userId;
    
    /**
     * 代金券id
     */
    private Long voucherId;
}
