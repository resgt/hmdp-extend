package com.hmdp.user.service.impl;

import com.hmdp.model.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.user.mapper.UserInfoMapper;
import com.hmdp.user.service.IUserInfoService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-24
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
