package com.hmdp.user.service.dubbo.impl;

import com.hmdp.dubbo.service.UserDubboService;
import com.hmdp.model.entity.User;
import com.hmdp.user.mapper.UserMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@DubboService(version = "1.0.0")
public class UserDubboServiceImpl implements UserDubboService {
    
    @Autowired
    private UserMapper userMapper;

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public List<User> getUserList(List<Long> ids) {
        return userMapper.selectBatchIds(ids);
    }
}
