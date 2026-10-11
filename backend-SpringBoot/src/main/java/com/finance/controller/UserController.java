package com.finance.controller;

import com.finance.entity.User;
import com.finance.service.UserService;
import com.finance.util.JwtUtil;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody User user) {

        Map<String, Object> response = new HashMap<>();

        if (user.getUserEmail() == null || user.getUserPassword() == null) {
            response.put("message", "註冊時缺少email 或 password");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (user.getUserName() == null) {
            response.put("message", "註冊時缺少 username");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        boolean isSuccess = userService.signUp(user);
        if (isSuccess) {
            User registerUser = userService.findUserByEmail(user.getUserEmail());

            if (registerUser != null) {
                response.put("userId", registerUser.getUserId());
                response.put("userName", registerUser.getUserName());
                response.put("userEmail", registerUser.getUserEmail());
                response.put("userRole", registerUser.getUserRole());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                // 註冊成功但查詢失敗
                System.err.println("註冊成功但無法查詢到使用者: " + user.getUserEmail());
                response.put("message", "註冊成功，但伺服器內部錯誤，無法登入。");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } else {
            response.put("message", "註冊失敗,email已存在");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody User user) {

        Map<String, Object> response = new HashMap<>();

        if (user.getUserEmail() == null || user.getUserPassword() == null) {
            response.put("message", "登入時缺少email 或 password");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        User loggedInUser = userService.login(user);

        if (loggedInUser != null) {

            String token = JwtUtil.generateToken(loggedInUser);

            response.put("userId", loggedInUser.getUserId());
            response.put("userName", loggedInUser.getUserName());
            response.put("userEmail", loggedInUser.getUserEmail());
            response.put("userRole", loggedInUser.getUserRole());
            response.put("userPassword", null);
            response.put("token", token);

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            response.put("message", "登入失敗，帳號或密碼錯誤");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "登出成功");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @RequestAttribute(value = "validateUser", required = false) User user) {

        Map<String, Object> response = new HashMap<>();

        if (user != null) {

            response.put("userId", user.getUserId());
            response.put("userEmail", user.getUserEmail());
            response.put("userName", user.getUserName());
            response.put("userRole", user.getUserRole());

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            response.put("message", "請先登入");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}
