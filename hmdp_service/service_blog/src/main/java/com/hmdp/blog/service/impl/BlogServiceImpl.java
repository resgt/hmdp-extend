package com.hmdp.blog.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.blog.mapper.BlogMapper;
import com.hmdp.blog.service.IBlogService;
import com.hmdp.common.utils.RedisConstants;
import com.hmdp.common.utils.SystemConstants;
import com.hmdp.common.utils.UserHolder;
import com.hmdp.dubbo.service.generic.GenericUserService;
import com.hmdp.dubbo.service.generic.GenericFollowService;
import com.hmdp.model.dto.Result;
import com.hmdp.model.dto.ScrollResult;
import com.hmdp.model.dto.UserDTO;
import com.hmdp.model.entity.Blog;
import com.hmdp.model.entity.User;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @DubboReference
    private GenericUserService userService;

    @DubboReference
    private GenericFollowService followService;

    @Override
    public Result queryHotBlog(Integer current, HttpServletRequest request) {
        Page<Blog> blogPage = new Page<>(current, SystemConstants.MAX_PAGE_SIZE);
        this.page(blogPage);
        List<Blog> blogList = blogPage.getRecords();
        // 获取用户信息并封装到集合中
        blogList.forEach(blog -> {
            this.setNameAndIconWithUser(blog);
            this.isBlogLiked(blog, request);
        });
        return Result.ok(blogList);
    }

    @Override
    public Result queryMyBlog(Integer current, HttpServletRequest request) {
        // 获取登录用户
        UserDTO user = new UserHolder(stringRedisTemplate).getUser(request);
        // 根据用户查询
        Page<Blog> blogPage = new Page<Blog>(current, SystemConstants.MAX_PAGE_SIZE);
        this.page(blogPage, new QueryWrapper<Blog>().eq("user_id", user.getId()));
        // 获取当前页数据
        List<Blog> records = blogPage.getRecords();
        // 返回数据
        return Result.ok(records);
    }

    @Override
    public Result queryBlogById(Long id, HttpServletRequest request) {
        // 查询blog
        Blog blog = this.getById(id);
        if (blog == null) return Result.fail("笔记不存在");
        // 查询blog关联的用户
        this.setNameAndIconWithUser(blog);
        // 查询blog是否被点赞
        this.isBlogLiked(blog, request);
        // 返回笔记信息
        return Result.ok(blog);
    }

    @Override
    public Result likeBlog(Long id, HttpServletRequest request) {
        // 获取登录用户id
        Long userId = new UserHolder(stringRedisTemplate).getUser(request).getId();
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        // 判断当前登录用户是否已经点赞
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            // 未点赞，可以进行点赞
            boolean isSuccess = this.update().setSql("liked = liked + 1").eq("id", id).update();
            // 更新redis
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 已点赞，取消点赞
            boolean isSuccess = this.update().setSql("liked = liked - 1").eq("id", id).update();
            // 更新redis
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLike(Long id) {
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        // 查询top5的点赞用户
        Set<String> top5Set = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        // 判断是否为空
        if (top5Set == null || top5Set.isEmpty()) return Result.ok();
        // 解析出其中的用户id
        List<Long> ids = new ArrayList<>();
        for (String top5 : top5Set) {
            ids.add(Long.valueOf(top5));
        }
        // 根据用户id查询用户
        List<UserDTO> userDtos = userService.getUserList(ids).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDtos);
    }

    @Override
    @Transactional
    public Result saveBlog(Blog blog, HttpServletRequest request) {
        // 获取当前登录用户
        Long userDtoId = new UserHolder(stringRedisTemplate).getUser(request).getId();
        blog.setUserId(userDtoId);
        // 保存探店博文
        boolean isSuccess = this.save(blog);
        if (!isSuccess) return Result.fail("新增笔记失败");
        // 查询笔记作者的所有粉丝
        List<Long> userIds = followService.getUserIdWithFollowUserId(userDtoId);
        for (Long userId : userIds) {
            String key = RedisConstants.FEED_KEY + userId;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
        }
        // 返回笔记id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset, HttpServletRequest request) {
        // 获取当前用户id
        Long userDtoId = new UserHolder(stringRedisTemplate).getUser(request).getId();
        // 滚动分页查询收件箱
        String key = RedisConstants.FEED_KEY + userDtoId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, max, offset, 3);
        // 判断是否为空
        if (typedTuples == null || typedTuples.isEmpty()) return Result.ok();
        // 解析数据
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0L;
        int os = 1;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            // 获取id
            Long id = Long.valueOf(typedTuple.getValue());
            ids.add(id);
            // 获取分数
            long time = typedTuple.getScore().longValue();
            if (time == minTime) {
                os++;
            } else {
                minTime = time;
                os = 1;
            }
        }
        // 根据id查询blog
        List<Blog> blogList = baseMapper.getBlogListWithOrder(ids);
        for (Blog blog : blogList) {
            // 查询blog关联的用户
            this.setNameAndIconWithUser(blog);
            // 查询blog是否被点赞
            this.isBlogLiked(blog, request);
        }
        // 封装
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogList);
        scrollResult.setMinTime(minTime);
        scrollResult.setOffset(os);
        // 返回数据
        return Result.ok(scrollResult);
    }

    // 判断用户点赞功能
    private void isBlogLiked(Blog blog, HttpServletRequest request) {
        // 获取登录用户
        UserDTO userDto = new UserHolder(stringRedisTemplate).getUser(request);
        // 用户未登录，直接返回
        if (userDto == null) return;
        Long userId = userDto.getId();
        // 判断当前登录用户是否已经点赞
        String key = RedisConstants.BLOG_LIKED_KEY + blog.getId();
        // 判断当前登录用户是否已经点赞
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    // 通过远程调用接口，完善name和icon
    private void setNameAndIconWithUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

}
