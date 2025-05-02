package com.hmdp.follow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.model.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FollowMapper extends BaseMapper<Follow> {
    List<Long> getUserIdWithFollowUserId(@Param("followUserId") Long followUserId);
}
