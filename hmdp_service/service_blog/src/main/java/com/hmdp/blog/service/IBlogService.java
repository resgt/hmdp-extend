package com.hmdp.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.model.dto.Result;
import com.hmdp.model.entity.Blog;

import javax.servlet.http.HttpServletRequest;

public interface IBlogService extends IService<Blog> {
    Result queryHotBlog(Integer current, HttpServletRequest request);

    Result queryMyBlog(Integer current, HttpServletRequest request);

    Result queryBlogById(Long id, HttpServletRequest request);

    Result likeBlog(Long id, HttpServletRequest request);

    Result queryBlogLike(Long id);

    Result saveBlog(Blog blog, HttpServletRequest request);

    Result queryBlogOfFollow(Long max, Integer offset, HttpServletRequest request);
}
