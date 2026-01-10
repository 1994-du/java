package com.springbootproject.Security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springbootproject.Util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        // 添加调试日志
        System.out.println("JwtAuthenticationEntryPoint被调用，请求路径: " + request.getRequestURI());
        System.out.println("请求方法: " + request.getMethod());
        System.out.println("认证异常: " + authException.getMessage());
        System.out.println("请求头Authorization: " + request.getHeader("Authorization"));
        
        // 获取请求头中的token，支持有Bearer前缀和没有前缀的两种情况
        String authorizationHeader = request.getHeader("Authorization");
        String token = null;
        if (authorizationHeader != null) {
            if (authorizationHeader.startsWith("Bearer ")) {
                token = authorizationHeader.substring(7);
            } else {
                token = authorizationHeader;
            }
        }

        // 设置响应状态和内容类型
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        // 创建响应内容
        Map<String, Object> responseBody = new HashMap<>();
        
        // 判断是否是token过期错误
        if (token != null && jwtUtils.isTokenExpiredError(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            responseBody.put("success", false);
            responseBody.put("status", 401);
            responseBody.put("message", "Token已过期，请重新登录");
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
            responseBody.put("success", false);
            responseBody.put("status", 403);
            responseBody.put("message", "未授权访问，请先登录");
        }

        // 写入响应
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), responseBody);
    }
}