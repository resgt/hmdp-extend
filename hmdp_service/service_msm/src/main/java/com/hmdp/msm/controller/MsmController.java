package com.hmdp.msm.controller;

import com.hmdp.model.utils.Mail;
import com.hmdp.msm.service.SendMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/msm")
public class MsmController {

    @Autowired
    private SendMailService sendMailService;

    @PostMapping("/inner/sendMail")
    public void sendMail(@RequestBody Mail mail) {
        sendMailService.sendMail(mail);
    }

}
