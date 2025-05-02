package com.hmdp.msm.service.dubbo.impl;

import com.hmdp.dubbo.service.MsmDubboService;
import com.hmdp.model.utils.Mail;
import com.hmdp.msm.service.SendMailService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@DubboService(version = "1.0.0")
public class MsmDubboServiceImpl implements MsmDubboService {
    
    @Autowired
    private SendMailService msmService;

    @Override
    public void sendMail(Mail mail) {
        msmService.sendMail(mail);
    }
}
