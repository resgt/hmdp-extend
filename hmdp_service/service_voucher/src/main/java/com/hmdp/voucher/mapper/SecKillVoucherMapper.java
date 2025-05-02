package com.hmdp.voucher.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.model.entity.SeckillVoucher;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SecKillVoucherMapper extends BaseMapper<SeckillVoucher> {

    boolean updateStockWithOptimisticLock(Long voucherId, Integer stock);
}
