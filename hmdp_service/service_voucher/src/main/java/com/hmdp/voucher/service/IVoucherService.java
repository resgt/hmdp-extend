package com.hmdp.voucher.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.model.dto.Result;
import com.hmdp.model.entity.Voucher;

public interface IVoucherService extends IService<Voucher> {
    void addSecKillVoucher(Voucher voucher);

    Result queryVoucherOfShop(Long shopId);
}
