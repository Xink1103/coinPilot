package com.finance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance.entity.User;
import com.finance.service.UserService;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @Autowired
    private UserService userService;

    @GetMapping("/count")
    public int getCount(){
        return userService.countTotalUsers();
    }

    @GetMapping("/user/{email}")
    public User getUser(@PathVariable String email){
        return userService.findUserByEmail(email);
    }

}
