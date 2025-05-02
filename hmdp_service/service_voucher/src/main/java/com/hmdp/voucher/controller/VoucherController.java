package com.hmdp.voucher.controller;

import com.hmdp.model.dto.Result;
import com.hmdp.model.entity.Voucher;
import com.hmdp.voucher.service.IVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Autowired
    private IVoucherService voucherService;

    /**
     * 新增秒杀劵
     * @param voucher
     * @return
     */
    @PostMapping("/seckill")
    public Result addSecKillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSecKillVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 查询店铺的优惠券列表
     * @param shopId 店铺id
     * @return 优惠券列表
     */
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
        return voucherService.queryVoucherOfShop(shopId);
    }

}
