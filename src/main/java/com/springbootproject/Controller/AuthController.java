package com.springbootproject.Controller;

import com.springbootproject.Model.LoginRequest;
import com.springbootproject.Model.RegisterRequest;
import com.springbootproject.Model.ResetPasswordRequest;
import com.springbootproject.Util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 直接注入主数据源，用于连接localhost数据库进行验证
    @Autowired
    private DataSource dataSource;
    
    // 注入密码编码器，用于密码加密
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // 注入JWT工具类，用于生成token
    @Autowired
    private JwtUtils jwtUtils;

    // 登录接口
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // 从请求对象中获取用户名和密码
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        
        try (Connection connection = dataSource.getConnection();
             // 直接查询user表，因为数据源已经连接到localhost数据库
             PreparedStatement stmt = connection.prepareStatement(
                     "SELECT * FROM user WHERE username = ?")) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {                
                String storedPassword = rs.getString("password");
                
                // 先尝试BCrypt匹配
                boolean bCryptMatch = passwordEncoder.matches(password, storedPassword);
                
                // 检查是否为明文密码匹配
                boolean plainTextMatch = password.equals(storedPassword);
                
                boolean passwordMatches = bCryptMatch || plainTextMatch;
                
                if (passwordMatches) {
                    try {
                        // 登录成功，生成token并返回
                        System.out.println("密码验证成功，准备生成token，用户: " + username);
                        String token = jwtUtils.generateToken(username);
                        System.out.println("Token生成成功: " + (token != null && token.length() > 0 ? "是" : "否"));
                        
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("status", 200);
                        response.put("message", "登录成功");
                        response.put("username", username);
                        response.put("token", token);
                        response.put("tokenExpiration", "2小时"); // token过期时间描述
                        
                        System.out.println("登录响应准备完成，即将返回");
                        return ResponseEntity.ok(response);
                    } catch (Exception e) {
                        System.out.println("Token生成异常: " + e.getClass().getName());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("密码验证失败，用户名: " + username);
                }
            }
        } catch (Exception e) {
            // 记录异常日志
            e.printStackTrace();
        }
        
        // 登录失败，返回状态码和错误消息
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("status", 400);
        response.put("message", "用户名或密码错误");
        
        return ResponseEntity.badRequest().body(response);
    }
    
    // 注册接口
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        // 从请求对象中获取用户名和密码
        String username = registerRequest.getUsername();
        String password = registerRequest.getPassword();
        
        // 基本验证
        if (username == null || username.trim().isEmpty() || password == null || password.length() < 6) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("status", 400);
            response.put("message", "用户名不能为空，密码长度不能少于6位");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // 检查用户名是否已存在
            boolean usernameExists = false;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(
                         "SELECT * FROM user WHERE username = ?")) {
                
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                usernameExists = rs.next();
            }
            
            if (usernameExists) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("status", 400);
                response.put("message", "用户名已存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 对密码进行加密
            String encodedPassword = passwordEncoder.encode(password);
            
            // 插入新用户
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(
                         "INSERT INTO user (username, password) VALUES (?, ?)")) {
                
                stmt.setString(1, username);
                stmt.setString(2, encodedPassword);
                stmt.executeUpdate();
                
                // 注册成功，返回状态码和成功消息
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("status", 200);
                response.put("message", "注册成功");
                response.put("username", username);
                
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            // 记录异常日志
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("status", 500);
            response.put("message", "注册失败，请稍后重试");
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // 密码重置接口
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {
        // 从请求对象中获取用户名和新密码
        String username = resetPasswordRequest.getUsername();
        String newPassword = resetPasswordRequest.getNewPassword();
        
        // 基本验证
        if (username == null || username.trim().isEmpty() || newPassword == null || newPassword.length() < 6) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("status", 400);
            response.put("message", "用户名不能为空，新密码长度不能少于6位");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // 检查用户是否存在
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(
                         "SELECT * FROM user WHERE username = ?")) {
                
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                
                if (!rs.next()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("status", 400);
                    response.put("message", "用户名不存在");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // 对新密码进行加密
            String encodedPassword = passwordEncoder.encode(newPassword);
            
            // 更新用户密码
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(
                         "UPDATE user SET password = ? WHERE username = ?")) {
                
                stmt.setString(1, encodedPassword);
                stmt.setString(2, username);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    // 密码重置成功
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("status", 200);
                    response.put("message", "密码重置成功");
                    response.put("username", username);
                    
                    return ResponseEntity.ok(response);
                } else {
                    // 更新失败
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("status", 500);
                    response.put("message", "密码重置失败，请稍后重试");
                    return ResponseEntity.status(500).body(response);
                }
            }
        } catch (Exception e) {
            // 记录异常日志
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("status", 500);
            response.put("message", "密码重置失败，请稍后重试");
            return ResponseEntity.status(500).body(response);
        }
    }
}