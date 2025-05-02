package com.hmdp.follow.service.dubbo.impl;

import com.hmdp.dubbo.service.FollowDubboService;
import com.hmdp.follow.service.IFollowService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@DubboService(version = "1.0.0")
public class FollowDubboServiceImpl implements FollowDubboService {
    
    @Autowired
    private IFollowService followService;

    @Override
    public List<Long> getUserIdWithFollowUserId(Long followUserId) {
        return followService.getUserIdWithFollowUserId(followUserId);
    }
}
