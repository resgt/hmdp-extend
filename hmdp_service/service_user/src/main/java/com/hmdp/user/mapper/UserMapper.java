package com.hmdp.user.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    // 根据手机号获取用户信息
    public User getByPhone(@Param("phone") String phone);

    List<User> getUserListWithOrder(@Param("ids") List<Long> ids);
}
