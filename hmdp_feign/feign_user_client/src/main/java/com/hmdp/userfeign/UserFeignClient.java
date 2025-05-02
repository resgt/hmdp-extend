package com.hmdp.userfeign;

import com.hmdp.model.dto.UserDTO;
import com.hmdp.model.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@FeignClient(name = "service-user", path = "/user")
public interface UserFeignClient {

    @GetMapping("/inner/{id}")
    public User getById(@PathVariable("id") Long id);

    @GetMapping("/inner/getUserList")
    public List<User> getUserList(@RequestParam List<Long> ids);
}
