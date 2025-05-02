package com.hmdp.dubbo.service.generic;

import com.hmdp.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenericUserService {
    
    private static final String INTERFACE_NAME = "com.hmdp.dubbo.service.UserDubboService";
    private static final String VERSION = "1.0.0";
    
    @Autowired
    private DubboGenericService dubboGenericService;
    
    @SuppressWarnings("unchecked")
    public User getById(Long id) {
        return (User) dubboGenericService.invoke(
            INTERFACE_NAME,
            VERSION,
            "getById",
            new String[]{"java.lang.Long"},
            new Object[]{id}
        );
    }
    
    @SuppressWarnings("unchecked")
    public List<User> getUserList(List<Long> ids) {
        return (List<User>) dubboGenericService.invoke(
            INTERFACE_NAME,
            VERSION,
            "getUserList",
            new String[]{"java.util.List"},
            new Object[]{ids}
        );
    }
}
