package com.finance.service;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.finance.entity.User;
import com.finance.repository.UserDAO;

@Service 
public class UserService {

    @Autowired
    private UserDAO userDAO;

    // 註冊邏輯
    public boolean signUp(User user) {
        try {
            // 檢查帳號是否已存在
            if (userDAO.findUserByEmail(user.getUserEmail()) != null) {
                System.err.println("帳號已存在，重複註冊");
                return false;
            }

            // 用單向雜湊函數加密密碼
            String hashedPassword = BCrypt.hashpw(user.getUserPassword(), BCrypt.gensalt());
            user.setUserPassword(hashedPassword);

            // 確定使用者在註冊後是 USER 的身份
            user.setUserRole("USER");

            // 新增到資料庫
            userDAO.insert(user);
            return true;

        } catch (RuntimeException e) {
            System.err.println("註冊過程發生問題: " + e.getMessage());
            return false;
        }
    }

    // 登入邏輯
    public User login(User user) {
        try {
            // 從資料庫查詢用戶
            User dbUser = userDAO.findUserByEmail(user.getUserEmail());

            if (dbUser == null) {
                return null;
            }

            // 用 BCrypt 比對密碼
            if (BCrypt.checkpw(user.getUserPassword(), dbUser.getUserPassword())) {
                // 登入成功，清除密碼後返回
                dbUser.setUserPassword(null);
                return dbUser;
            }

        } catch (RuntimeException e) {
            System.err.println("登入過程發生問題: " + e.getMessage());
        }
        return null;
    }

    // 透過 Email 查詢用戶
    public User findUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        try {
            User user = userDAO.findUserByEmail(email);
            if (user != null) {
                // 清除密碼
                user.setUserPassword(null);
            }
            return user;

        } catch (RuntimeException e) {
            System.err.println("伺服器錯誤: " + e.getMessage());
        }
        return null;
    }

    // 透過 ID 查詢用戶  JWT Token 會用到
    public User findUserById(int userId) {
        if (userId <= 0) {
            return null;
        }

        try {
            User user = userDAO.findUserById(userId);
            if (user != null) {
                // 清除密碼
                user.setUserPassword(null);
            }
            return user;

        } catch (RuntimeException e) {
            System.err.println("透過 userId 查詢使用者時發生伺服器錯誤: " + e.getMessage());
        }
        return null;
    }
}