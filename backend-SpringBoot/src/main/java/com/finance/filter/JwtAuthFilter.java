package com.finance.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.finance.entity.User;
import com.finance.service.UserService;
import com.finance.util.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter implements Filter {

    @Autowired
    private UserService UserService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        //過濾白名單
        if(shouldSkipFilter(requestURI, method)){
            chain.doFilter(httpRequest, httpResponse);
            return;
        }

        String authHeader = httpRequest.getHeader("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            sendAuthorizationResponse(httpResponse, "未提共有效的token");
            return;  
        }

        Claims claims = JwtUtil.validateToken(authHeader);
        if(claims == null){
            sendAuthorizationResponse(httpResponse, "token無效或已過期");
            return;
        }

        Integer userId = claims.get("userId", Integer.class);
        if(userId == null ){
            sendAuthorizationResponse(httpResponse, "token未包含userId");
            return;  
        }

        User validateUser = UserService.findUserById(userId);
        if(validateUser == null){
            sendAuthorizationResponse(httpResponse, "user不存在");
            return;  
        }

        httpRequest.setAttribute("validateUser", validateUser);
        
        chain.doFilter(request, response);
    }

    //輔助方法過濾名單
    public boolean shouldSkipFilter(String uri, String method){
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 因為有 context path，所以 URI 會是 /finanace_system/api/
        String pathWithoutContext = uri.replaceFirst("^/finanace_system", "");

        return pathWithoutContext.endsWith("/login") ||
               pathWithoutContext.endsWith("/signup") ||
               pathWithoutContext.endsWith("/snapshot") ||
               pathWithoutContext.startsWith("/swagger-ui") ||
               pathWithoutContext.startsWith("/v3/api-docs") ||
               pathWithoutContext.startsWith("/error");
    }

    //401 error
    public void sendAuthorizationResponse(HttpServletResponse response, String message) throws IOException{
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
    
}