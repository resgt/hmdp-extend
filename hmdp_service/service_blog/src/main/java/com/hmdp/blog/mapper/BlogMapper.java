package com.hmdp.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.model.entity.Blog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BlogMapper extends BaseMapper<Blog> {
    List<Blog> getBlogListWithOrder(List<Long> ids);
}
