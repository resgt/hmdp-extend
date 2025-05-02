package com.hmdp.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.common.utils.RedisConstants;
import com.hmdp.common.utils.RegexUtils;
import com.hmdp.common.utils.UserHolder;
import com.hmdp.model.dto.LoginFormDTO;
import com.hmdp.model.dto.Result;
import com.hmdp.model.dto.UserDTO;
import com.hmdp.model.entity.User;
import com.hmdp.user.mapper.UserMapper;
import com.hmdp.user.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.common.utils.RedisConstants.*;
import static com.hmdp.common.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送短信验证码并保存验证码
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 校验手机号格式
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }

        // 生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 验证码保存到redis并设置有效时间
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 5.发送验证码
        log.debug("发送短信验证码成功，验证码：{}", code);

        // 返回ok
        return Result.ok();
    }

    /**
     * 手机登录
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        // 校验手机号格式
        if (RegexUtils.isPhoneInvalid(phone)) return Result.fail("手机号格式错误");
        // 校验手机验证码是否正确
        if (cacheCode == null || !cacheCode.equals(loginForm.getCode())) return Result.fail("手机验证码错误");
        // 根据手机号查询用户
        User user = baseMapper.getByPhone(phone);
        // 判断用户是否存在
        if (user == null) {
            // 不存在则创建新用户
            user = createNewUserWithPhone(phone);
        }
        // user对象转为userdto对象
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 生成随机token，作为登录校验令牌
        String token = UUID.randomUUID().toString(true);
        // 将userDTO转为hashMap
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString())
        );
        // 将userMap存储到redis中
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userMap);
        // 设置token有效期（有效期过后将自动退出登录状态，这里我设置的是60分钟）
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    /**
     * 获取用户信息
     * @return
     */
    @Override
    public UserDTO getMe(HttpServletRequest request) {
        String token = request.getHeader("authorization");
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        return userDTO;
    }

    @Override
    public void logout(HttpServletRequest request) {
        new UserHolder(stringRedisTemplate).removeUser(request);
    }

    @Override
    public List<User> getUserListWithOrder(List<Long> ids) {
        List<User> userList = baseMapper.getUserListWithOrder(ids);
        return userList;
    }

    @Override
    public Result sign(HttpServletRequest request) {
        // 获取当前登录用户id
        Long userId = new UserHolder(stringRedisTemplate).getUser(request).getId();
        // 获取当前日期
        LocalDateTime nowDate = LocalDateTime.now();
        // 获取当前的年份和月份
        String yearMonth = nowDate.format(DateTimeFormatter.ofPattern("yyyy-NN"));
        // 拼接key
        String key = USER_SIGN_KEY + userId + ":" + yearMonth;
        // 获取本月的第几天
        int dayOfMonth = nowDate.getDayOfMonth();
        // 写入redis
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount(HttpServletRequest request) {
        // 获取当前登录用户id
        Long userId = new UserHolder(stringRedisTemplate).getUser(request).getId();
        // 获取当前日期
        LocalDateTime nowDate = LocalDateTime.now();
        // 获取当前的年份和月份
        String yearMonth = nowDate.format(DateTimeFormatter.ofPattern("yyyy-NN"));
        // 拼接key
        String key = USER_SIGN_KEY + userId + ":" + yearMonth;
        // 获取本月的第几天
        int dayOfMonth = nowDate.getDayOfMonth();
        // 获取本月到今天为止的所有的签到记录，返回的是一个十进制的数字
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0)
        );
        if (request == null || result.isEmpty()) return Result.ok(0);
        Long num = result.get(0);
        if (num == null || num == 0) return Result.ok(0);
        // 签到天数计数器
        int count = 0;
        // 遍历
        while (true) {
            // 与 1 做与运算，得到数字的最后一个bit位
            if ((num & 1) == 0) {
                break;
            } else {
                count++;
            }
            // 将num右移一位，得到前一个bit位
            num >>>= 1;
        }
        return Result.ok(count);
    }

    /**
     * 根据手机号创建新的用户
     * @param phone
     * @return
     */
    private User createNewUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 保存到数据库
        this.save(user);
        return user;
    }
}
