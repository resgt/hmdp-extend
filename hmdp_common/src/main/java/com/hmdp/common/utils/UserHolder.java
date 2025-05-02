package com.hmdp.common.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.model.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.servlet.http.HttpServletRequest;

import java.util.Map;

import static com.hmdp.common.utils.RedisConstants.LOGIN_USER_KEY;

/**
 * 目前我无法在微服务项目中整合ThreadLocal，所以使用redis做代替
 */
public class UserHolder {
//    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();
//
//    public static void saveUser(UserDTO user){
//        tl.set(user);
//    }
//
//    public static UserDTO getUser(){
//        return tl.get();
//    }
//
//    public static void removeUser(){
//        tl.remove();
//    }
    private StringRedisTemplate stringRedisTemplate;

    public UserHolder(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 获取当前登录用户
    public UserDTO getUser(HttpServletRequest request) {
        String token = request.getHeader("authorization");
        if (token == null) return null;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        return userDTO;
    }

    // 移除当前登录用户
    public void removeUser(HttpServletRequest request) {
        String token = request.getHeader("authorization");
        stringRedisTemplate.delete(LOGIN_USER_KEY + token);
    }
}
