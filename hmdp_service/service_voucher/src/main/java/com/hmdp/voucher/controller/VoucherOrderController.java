package com.hmdp.voucher.controller;

import com.hmdp.model.dto.Result;
import com.hmdp.voucher.service.IVoucherOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Autowired
    private IVoucherOrderService voucherOrderService;

    /**
     * 秒杀劵抢购
     * @param voucherId
     * @param request
     * @return
     */
    @PostMapping("/seckill/{id}")
    public Result orderSecKillVoucher(@PathVariable("id") Long voucherId, HttpServletRequest request) {
        return voucherOrderService.orderSecKillVoucherWithLua(voucherId, request);
    }

}
