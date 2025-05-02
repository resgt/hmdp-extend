package com.hmdp.follow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.model.dto.Result;
import com.hmdp.model.entity.Follow;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IFollowService extends IService<Follow> {
    Result followUser(Long followUserId, Boolean isFollow, HttpServletRequest request);

    Result isFollow(Long followUserId, HttpServletRequest request);

    Result followCommons(Long id, HttpServletRequest request);

    List<Long> getUserIdWithFollowUserId(Long followUserId);
}
