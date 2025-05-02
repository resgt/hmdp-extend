package com.hmdp.msmfeign;

import com.hmdp.model.utils.Mail;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "service-msm", path = "/msm")
public interface MsmFeignClient {

    @PostMapping("/inner/sendMail")
    public void sendMail(Mail mail);
}
