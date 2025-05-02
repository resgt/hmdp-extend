package com.hmdp.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.blog.service.IBlogService;
import com.hmdp.common.utils.SystemConstants;
import com.hmdp.common.utils.UserHolder;
import com.hmdp.model.dto.Result;
import com.hmdp.model.dto.UserDTO;
import com.hmdp.model.entity.Blog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/blog")
public class BlogController {

    @Autowired
    private IBlogService blogService;

    @PostMapping
    public Result saveBlog(@RequestBody Blog blog, HttpServletRequest request) {
        return blogService.saveBlog(blog, request);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current, HttpServletRequest request) {
        return blogService.queryHotBlog(current, request);
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current, HttpServletRequest request) {
        return blogService.queryMyBlog(current, request);
    }

    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id, HttpServletRequest request) {
        return blogService.queryBlogById(id, request);
    }

    @PutMapping("/like/{id}")
    public Result LikeBlog(@PathVariable("id") Long id, HttpServletRequest request) {
        return blogService.likeBlog(id, request);
    }

    @GetMapping("/likes/{id}")
    public Result queryBlogLike(@PathVariable("id") Long id) {
        return blogService.queryBlogLike(id);
    }

    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {
        // 根据用户查询
        Page<Blog> page = new Page<>(current, SystemConstants.MAX_PAGE_SIZE);
        blogService.page(page, new QueryWrapper<Blog>().eq("user_id", id));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(@RequestParam("lastId") Long max,
                                    @RequestParam(value = "offset", defaultValue = "0") Integer offset,
                                    HttpServletRequest request) {
        return blogService.queryBlogOfFollow(max, offset, request);
    }


}
