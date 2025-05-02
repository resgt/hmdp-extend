package com.hmdp.user.controller;


import cn.hutool.core.bean.BeanUtil;
import com.hmdp.common.utils.UserHolder;
import com.hmdp.model.dto.LoginFormDTO;
import com.hmdp.model.dto.Result;
import com.hmdp.model.dto.UserDTO;
import com.hmdp.model.entity.User;
import com.hmdp.model.entity.UserInfo;
import com.hmdp.user.service.IUserInfoService;
import com.hmdp.user.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.message.ReusableMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    @Autowired
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        // 发送短信验证码并保存验证码
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        // 实现登录功能
        return userService.login(loginForm, session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request){
        userService.logout(request);
        return Result.ok();
    }

    @GetMapping("/me")
    public Result me(HttpServletRequest request){
        // 获取当前登录的用户并返回
        UserDTO userDto = userService.getMe(request);
        return Result.ok(userDto);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }

    /**
     * 签到
     * @param request
     * @return
     */
    @PostMapping("/sign")
    public Result sign(HttpServletRequest request){
        return userService.sign(request);
    }

    /**
     * 统计连续签到次数
     * @param request
     * @return
     */
    @GetMapping("/sign/count")
    public Result signCount(HttpServletRequest request){
        return userService.signCount(request);
    }

    /**
     * 内部远程调用接口
     * @param id
     * @return
     */
    @GetMapping("/inner/{id}")
    public User getById(@PathVariable("id") Long id) {
        User user = userService.getById(id);
        return user;
    }

    @GetMapping("/inner/getUserList")
    public List<User> getUserList(@RequestParam List<Long> ids) {
        return userService.getUserListWithOrder(ids);
    }
}