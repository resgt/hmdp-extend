package com.hmdp.followfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "service-follow", path = "/follow")
public interface FollowFeignClient {

    @PostMapping("/inner/getUserId")
    public List<Long> getUserIdWithFollowUserId(@RequestParam("userDtoId") Long followUserId);

}