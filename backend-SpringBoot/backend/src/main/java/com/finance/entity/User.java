package com.finance.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data //getter setter
@NoArgsConstructor //無參數建構子
@AllArgsConstructor //所有參數建構子
public  class User{

    private Integer userId;
    private String userName;
    private String userPassword;
    private String userEmail;
    private String userRole;

    public User(String userName,String userPassword,String userEmail){
        this.userName = userName;
        this.userPassword = userPassword;
        this.userEmail = userEmail;
    }

    public boolean isAdmin(){
        return "ADMIN".equalsIgnoreCase(userRole);
    }

    public boolean isUser(){
        return "USER".equalsIgnoreCase(userRole);
    }

}