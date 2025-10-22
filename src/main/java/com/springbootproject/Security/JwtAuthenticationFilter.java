package com.springbootproject.Security;

import com.springbootproject.Util.JwtUtils;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("处理请求: " + request.getMethod() + " " + request.getRequestURI());
        
        try {
            // 从请求头获取token
            String authorizationHeader = request.getHeader("Authorization");
            System.out.println("Authorization头: " + (authorizationHeader != null ? authorizationHeader.substring(0, Math.min(20, authorizationHeader.length())) + "..." : "null"));
            
            String token = null;
            String username = null;

            // 检查token格式并提取token
            if (authorizationHeader != null) {
                // 支持有Bearer前缀和没有前缀的两种情况
                if (authorizationHeader.startsWith("Bearer ")) {
                    token = authorizationHeader.substring(7);
                    System.out.println("检测到Bearer前缀，提取的token: " + (token != null && token.length() > 0 ? token.substring(0, 20) + "..." : "null"));
                } else {
                    // 直接使用整个Authorization头作为token
                    token = authorizationHeader;
                    System.out.println("未检测到Bearer前缀，直接使用整个Authorization头作为token: " + (token != null && token.length() > 0 ? token.substring(0, 20) + "..." : "null"));
                }
                
                try {
                    // 从token中提取用户名
                    username = jwtUtils.getUsernameFromToken(token);
                    System.out.println("从token中提取的用户名: " + username);
                } catch (Exception e) {
                    System.out.println("从token中提取用户名失败: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("Authorization头不存在");
            }

            // 如果token有效且用户未认证
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                System.out.println("准备验证token并设置认证信息");
                
                // 验证token，使用正确的双参数版本
                boolean isValid = jwtUtils.validateToken(token, username);
                System.out.println("Token验证结果: " + isValid);
                
                if (isValid) {
                    // 创建简单的UserDetails对象，只包含用户名
                    UserDetails userDetails = User.builder()
                            .username(username)
                            .password("") // 密码在token验证中不需要
                            .authorities(Collections.emptyList())
                            .build();
                    
                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    
                    // 将认证对象存入SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    System.out.println("认证成功，已设置SecurityContext，用户: " + username);
                } else {
                    System.out.println("Token验证失败，未设置认证信息");
                }
            } else {
                if (username == null) {
                    System.out.println("用户名提取失败，无法进行认证");
                } else {
                    System.out.println("用户已认证，跳过设置");
                }
            }
        } catch (JwtException e) {
            System.out.println("JWT异常: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("认证过程中发生未知异常: " + e.getMessage());
            e.printStackTrace();
        }

        // 继续过滤链
        filterChain.doFilter(request, response);
    }
}