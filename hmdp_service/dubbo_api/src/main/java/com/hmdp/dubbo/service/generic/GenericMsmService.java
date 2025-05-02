package com.hmdp.dubbo.service.generic;

import com.hmdp.model.utils.Mail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GenericMsmService {
    
    private static final String INTERFACE_NAME = "com.hmdp.dubbo.service.MsmDubboService";
    private static final String VERSION = "1.0.0";
    
    @Autowired
    private DubboGenericService dubboGenericService;
    
    public void sendMail(Mail mail) {
        dubboGenericService.invoke(
            INTERFACE_NAME,
            VERSION,
            "sendMail",
            new String[]{"com.hmdp.model.utils.Mail"},
            new Object[]{mail}
        );
    }
}
