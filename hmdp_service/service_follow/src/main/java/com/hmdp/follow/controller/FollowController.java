package com.hmdp.follow.controller;

import com.hmdp.follow.service.IFollowService;
import com.hmdp.model.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private IFollowService followService;

    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow, HttpServletRequest request) {
        return followService.followUser(followUserId, isFollow, request);
    }


    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId, HttpServletRequest request) {
        return followService.isFollow(followUserId, request);
    }

    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable Long id, HttpServletRequest request) {
        return followService.followCommons(id, request);
    }

    @PostMapping("/inner/getUserId")
    public List<Long> getUserIdWithFollowUserId(@RequestParam("userDtoId") Long followUserId) {
        return followService.getUserIdWithFollowUserId(followUserId);
    }
}
