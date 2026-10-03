package com.finance.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance.entity.Admin;
import com.finance.entity.User;
import com.finance.service.AdminService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getAdminDashboard(
            @RequestAttribute(value = "validateUser", required = false) User user) {

        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("message", "未授權存取，請先登入");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        if (!user.isAdmin()) {
            response.put("message", "權限不足：僅管理者可存取此資源");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        try {
            Admin adminData = adminService.getAdminDashboardData();
            return ResponseEntity.status(HttpStatus.OK).body(adminData);

        } catch (RuntimeException e) {
            System.err.println("管理者儀表板錯誤: " + e.getMessage());
            e.printStackTrace();
            
            response.put("message", "伺服器錯誤：無法取得儀表板數據");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}