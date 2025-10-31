package com.springbootproject.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 最简单的安全配置，允许所有请求
        http
            .csrf(csrf -> csrf.disable()) // 完全禁用CSRF保护
            .cors(cors -> cors.disable()) // 禁用CORS限制
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll() // 允许所有请求
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        System.out.println("安全配置: 已完全禁用安全限制，允许所有请求访问");

        return http.build();
    }
}