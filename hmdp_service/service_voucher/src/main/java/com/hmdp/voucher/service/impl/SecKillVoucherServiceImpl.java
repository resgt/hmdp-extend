package com.hmdp.voucher.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.model.entity.SeckillVoucher;
import com.hmdp.voucher.mapper.SecKillVoucherMapper;
import com.hmdp.voucher.service.ISecKillVoucherService;
import org.springframework.stereotype.Service;

@Service
public class SecKillVoucherServiceImpl extends ServiceImpl<SecKillVoucherMapper, SeckillVoucher> implements ISecKillVoucherService {


    @Override
    public boolean updateStockWithOptimisticLock(Long voucherId, Integer stock) {
        return baseMapper.updateStockWithOptimisticLock(voucherId, stock);
    }

}
