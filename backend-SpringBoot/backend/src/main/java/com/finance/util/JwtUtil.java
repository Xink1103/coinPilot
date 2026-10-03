package com.finance.util;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.finance.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;


@Component
public class JwtUtil {

    // 從 application.properties 讀取設定
    @Value("${jwt.secret:mySecretKey123456789012345678901234567890}")
    private String secretString;

    @Value("${jwt.expiration:432000000}")  // 5 天
    private long expirationTimeInstance;

    // Static 變數供 static 方法使用
    private static SecretKey SECRET_KEY;
    private static long EXPIRATION_TIME;

    @PostConstruct
    public void init() {
        // 設定金鑰
        if (secretString != null && !secretString.isEmpty()) {
            SECRET_KEY = Keys.hmacShaKeyFor(secretString.getBytes());
        } else {
            SECRET_KEY = Jwts.SIG.HS256.key().build();
        }
        
        EXPIRATION_TIME = this.expirationTimeInstance;
    }

    // 根據 User 物件產生 JWT Token
    public static String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

        String token = Jwts.builder()
                .claim("userId", user.getUserId())
                .claim("userRole", user.getUserRole())
                .subject(String.valueOf(user.getUserId()))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(SECRET_KEY)  
                .compact();

        return "Bearer " + token;
    }

    //驗證並解析 JWT Token
    public static Claims validateToken(String token) {
        try {
            // 移除 "Bearer " 前綴
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            return Jwts.parser()
                    .verifyWith(SECRET_KEY)      
                    .build()
                    .parseSignedClaims(token)    
                    .getPayload();               

        } catch (Exception e) {
            System.err.println("Token 驗證失敗: " + e.getMessage());
            return null;
        }
    }

    // 從 Token 取得 userId
    public static Integer getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return claims.get("userId", Integer.class);
        }
        return null;
    }

    // 從 Token 取得 userRole
    public static String getUserRoleFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return claims.get("userRole", String.class);
        }
        return null;
    }

    // 檢查 Token 是否有效
    public static boolean isTokenValid(String token) {
        return validateToken(token) != null;
    }
}