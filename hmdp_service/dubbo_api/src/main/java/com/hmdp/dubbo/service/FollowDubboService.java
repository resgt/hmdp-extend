package com.hmdp.dubbo.service;

import java.util.List;

public interface FollowDubboService {
    List<Long> getUserIdWithFollowUserId(Long followUserId);
}
