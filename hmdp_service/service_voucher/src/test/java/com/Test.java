package com;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.model.entity.SeckillVoucher;
import com.hmdp.model.utils.Mail;
import com.hmdp.msmfeign.MsmFeignClient;
import com.hmdp.voucher.VoucherApplication;
import com.hmdp.voucher.service.ISecKillVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = VoucherApplication.class)
public class Test {

    @Autowired
    private MsmFeignClient msmFeignClient;

    @Autowired
    private ISecKillVoucherService secKillVoucherService;

    private String toOne = "2569362172@qq.com";

    private String subject = "测试邮件";

    private String context = "你好，测试邮件";

    @org.junit.jupiter.api.Test
    public void test() {
        Mail mail = new Mail();
        mail.setToOne(toOne);
        mail.setSubject(subject);
        mail.setContext(context);
        msmFeignClient.sendMail(mail);
    }
}
