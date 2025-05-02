package com.hmdp.voucher.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.common.utils.RedisConstants;
import com.hmdp.model.dto.Result;
import com.hmdp.model.entity.SeckillVoucher;
import com.hmdp.model.entity.Voucher;
import com.hmdp.voucher.mapper.VoucherMapper;
import com.hmdp.voucher.service.ISecKillVoucherService;
import com.hmdp.voucher.service.IVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Autowired
    private ISecKillVoucherService secKillVoucherService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addSecKillVoucher(Voucher voucher) {
        // 保存优惠劵
        this.save(voucher);
        // 保存秒杀劵
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        secKillVoucherService.save(seckillVoucher);
        // 保存秒杀库存到redis中
//        stringRedisTemplate.opsForValue().set(RedisConstants.SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
    }

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = baseMapper.queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

}
