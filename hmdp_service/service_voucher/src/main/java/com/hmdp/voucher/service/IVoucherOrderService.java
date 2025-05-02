package com.hmdp.voucher.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.model.dto.Result;
import com.hmdp.model.entity.VoucherOrder;

import javax.servlet.http.HttpServletRequest;

public interface IVoucherOrderService extends IService<VoucherOrder> {
    Result orderSecKillVoucher(Long voucherId, HttpServletRequest request);

    Result orderSecKillVoucherWithLua(Long voucherId, HttpServletRequest request);

    void removeByStatus();

    Result createVoucherOrder(Long voucherId, Long userId);

    void createVoucherOrderWithLua(VoucherOrder voucherOrder);
}
