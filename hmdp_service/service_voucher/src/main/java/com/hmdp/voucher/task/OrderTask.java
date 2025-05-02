package com.hmdp.voucher.task;

import com.hmdp.dubbo.service.generic.GenericMsmService;
import com.hmdp.model.utils.Mail;
import com.hmdp.voucher.service.IVoucherOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单的定时任务
 */
@Component
@EnableScheduling
public class OrderTask {

    @Autowired
    private GenericMsmService msmService;

    @Autowired
    private IVoucherOrderService voucherOrderService;

    private static final String TO_ONE = "2569362172@qq.com";

    /**
     * 每天0点执行方法，邮件提醒用户订单未支付，将自动删除
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void noticeNotPaidOrder() {
        Mail mail = createMail();
        msmService.sendMail(mail);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void deleteNotPaidOrder() {
        voucherOrderService.removeByStatus();
    }

    /**
     * 获取短信信息
     * @return
     */
    private Mail createMail() {
        Mail mail = new Mail();
        mail.setSubject("订单支付提醒");
        mail.setContext("您有订单还未支付，将在24小时后自动删除");
        mail.setToOne(TO_ONE);
        return mail;
    }
}
