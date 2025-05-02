package com.hmdp.follow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.common.utils.UserHolder;
import com.hmdp.dubbo.service.generic.GenericUserService;
import com.hmdp.follow.mapper.FollowMapper;
import com.hmdp.follow.service.IFollowService;
import com.hmdp.model.dto.Result;
import com.hmdp.model.dto.UserDTO;
import com.hmdp.model.entity.Follow;
import com.hmdp.model.entity.User;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @DubboReference
    private GenericUserService userService;

    @Override
    public Result followUser(Long followUserId, Boolean isFollow, HttpServletRequest request) {
        // 获取当前登录用户
        Long userId = new UserHolder(stringRedisTemplate).getUser(request).getId();
        String key = "follows:" + userId;
        // 判断是关注还是取关
        if (isFollow) {
            // 关注，新增数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = this.save(follow);
            if (isSuccess) {
                // 把关注用户的id放入redis的set集合中
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }
        } else {
            // 取关，删除数据
            boolean isSuccess = this.remove(new QueryWrapper<Follow>().eq("user_Id", userId).eq("follow_user_id", followUserId));
            if (isSuccess) {
                // 移除关注的用户id
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId, HttpServletRequest request) {
        // 获取当前登录用户
        Long userId = new UserHolder(stringRedisTemplate).getUser(request).getId();
        // 判断是否关注
        int count = this.count(new QueryWrapper<Follow>().eq("user_Id", userId).eq("follow_user_id", followUserId));
        // 返回判断结果
        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long id, HttpServletRequest request) {
        // 获取当前登录用户
        Long userId = new UserHolder(stringRedisTemplate).getUser(request).getId();
        // 当前登录用户的key
        String key1 = "follows:" + userId;
        // 需要查看用户的key
        String key2 = "follows:" + id;
        // 得到交集
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if (intersect == null || intersect.isEmpty()) return Result.ok(null);
        // 解析出id
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        // 查询用户
        List<UserDTO> userDtos = userService.getUserList(ids).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        // 返回用户信息
        return Result.ok(userDtos);
    }

    @Override
    public List<Long> getUserIdWithFollowUserId(Long followUserId) {
        return baseMapper.getUserIdWithFollowUserId(followUserId);
    }
}
