package com.finance.config;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:5173", // Vue.js
                        "http://127.0.0.1:5173" // 本地 IP
                        )
                // 允許的 HTTP 方法，OPTION的概念是一個預先的請求，讓瀏覽器可以回傳一個OPTION的時候可以先訊問相關的內容
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                // 允許的 Header (包含 Authorization)
                .allowedHeaders("*")
                // 會讓瀏覽器回傳Session ID 或是 JWT Token
                .allowCredentials(true)
                // 預檢請求的快取時間 (秒)
                .maxAge(3600);
    }
}
