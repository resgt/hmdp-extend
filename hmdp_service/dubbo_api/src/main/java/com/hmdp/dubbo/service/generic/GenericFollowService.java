package com.hmdp.dubbo.service.generic;

import com.hmdp.dubbo.service.FollowDubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenericFollowService {
    
    private static final String INTERFACE_NAME = "com.hmdp.dubbo.service.FollowDubboService";
    private static final String VERSION = "1.0.0";
    
    @Autowired
    private DubboGenericService dubboGenericService;
    
    @SuppressWarnings("unchecked")
    public List<Long> getUserIdWithFollowUserId(Long followUserId) {
        return (List<Long>) dubboGenericService.invoke(
            INTERFACE_NAME,
            VERSION,
            "getUserIdWithFollowUserId",
            new String[]{"java.lang.Long"},
            new Object[]{followUserId}
        );
    }
}
