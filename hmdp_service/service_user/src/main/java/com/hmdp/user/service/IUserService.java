package com.hmdp.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.model.dto.LoginFormDTO;
import com.hmdp.model.dto.Result;
import com.hmdp.model.dto.UserDTO;
import com.hmdp.model.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    UserDTO getMe(HttpServletRequest request);

    void logout(HttpServletRequest request);

    List<User> getUserListWithOrder(List<Long> ids);

    Result sign(HttpServletRequest request);

    Result signCount(HttpServletRequest request);
}
