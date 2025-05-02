package com.hmdp.msm.service.impl;

import com.hmdp.model.utils.Mail;
import com.hmdp.msm.service.SendMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
@Service
public class SendMailServiceImpl implements SendMailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void sendMail(Mail mail) {
        SimpleMailMessage MESSAGE = new SimpleMailMessage();
        // 设置发件人信息
        MESSAGE.setFrom(from);
        // 设置收件人信息
        MESSAGE.setTo(mail.getToOne());
        // 设置邮件主题
        MESSAGE.setSubject(mail.getSubject());
        // 设置邮件内容（可以解析HTML文本）
        MESSAGE.setText(mail.getContext());
        // 发送邮件
        javaMailSender.send(MESSAGE);
    }
}
