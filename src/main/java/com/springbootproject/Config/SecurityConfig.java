package com.springbootproject.Config;

import com.springbootproject.Security.JwtAuthenticationEntryPoint;
import com.springbootproject.Security.JwtAuthenticationFilter;
import com.springbootproject.Util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtils, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 设置安全配置
        http
            .csrf(csrf -> csrf.disable()) // 禁用CSRF保护
            .cors(cors -> cors.disable()) // 禁用CORS限制
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(unauthorizedHandler) // 配置认证失败处理
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 无状态会话
            )
            .authorizeHttpRequests(authorize -> authorize
                // 允许不需要认证的请求
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/test-public").permitAll()
                .requestMatchers("/test-websocket").permitAll()
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            );

        // 注册JWT认证过滤器
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        System.out.println("安全配置: 已启用JWT认证，仅允许/api/auth/**和/api/public/**路径无需认证");

        return http.build();
    }
}