package com.finance.filter;

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
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthFilter implements Filter {

    @Autowired private UserService UserService;
    private static final Set<String> PUBLIC_ENDPOINTS =
            Set.of("POST /api/users/login", "POST /api/users/signup", "GET /api/prices/snapshot");
    private static final List<String> PUBLIC_PREFIXES =
            List.of("/swagger-ui", "/v3/api-docs", "/error");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 過濾白名單
        if (shouldSkipFilter(httpRequest)) {
            chain.doFilter(httpRequest, httpResponse);
            return;
        }

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendAuthorizationResponse(httpResponse, "未提供有效的token");
            return;
        }

        Claims claims = JwtUtil.validateToken(authHeader);
        if (claims == null) {
            sendAuthorizationResponse(httpResponse, "token無效或已過期");
            return;
        }

        Integer userId = claims.get("userId", Integer.class);
        if (userId == null) {
            sendAuthorizationResponse(httpResponse, "token未包含userId");
            return;
        }

        User validateUser = UserService.findUserById(userId);
        if (validateUser == null) {
            sendAuthorizationResponse(httpResponse, "user不存在");
            return;
        }

        httpRequest.setAttribute("validateUser", validateUser);
        chain.doFilter(request, response);
    }

    // 輔助方法過濾名單
    private boolean shouldSkipFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String apiPath = uri.substring(contextPath.length());
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        // 精確比對 method + apiPath，或符合公開前綴
        return PUBLIC_ENDPOINTS.contains(method + " " + apiPath)
                || PUBLIC_PREFIXES.stream().anyMatch(prefix -> apiPath.startsWith(prefix));
    }

    // 401 error
    public void sendAuthorizationResponse(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
