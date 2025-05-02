package com.hmdp.voucher.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.model.entity.SeckillVoucher;

public interface ISecKillVoucherService extends IService<SeckillVoucher> {

    // 乐观锁思想保证线程安全，修改库存
    boolean updateStockWithOptimisticLock(Long voucherId, Integer stock);

}
