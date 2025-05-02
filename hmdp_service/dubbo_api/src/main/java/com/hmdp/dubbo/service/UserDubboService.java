package com.hmdp.dubbo.service;

import com.hmdp.model.entity.User;
import java.util.List;

public interface UserDubboService {
    User getById(Long id);
    List<User> getUserList(List<Long> ids);
}
