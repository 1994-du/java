package com.springbootproject.Controller;

import com.springbootproject.Entity.Menu;
import com.springbootproject.Entity.User;
import com.springbootproject.Model.ApiResponse;
import com.springbootproject.Service.MenuService;
import com.springbootproject.Service.UserService;
import com.springbootproject.Util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 注入用户服务，用于用户验证
    @Autowired
    private UserService userService;
    
    // 注入密码编码器，用于密码加密
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // 注入JWT工具类，用于生成token
    @Autowired
    private JwtUtils jwtUtils;
    
    // 注入菜单服务，用于获取用户菜单数据
    @Autowired
    private MenuService menuService;

    // 登录接口
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> loginRequest, HttpServletResponse response) {
        // 从请求对象中获取用户名和密码
        String username = (String) loginRequest.get("username");
        String password = (String) loginRequest.get("password");
        
        // 添加空值检查
        if (loginRequest == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("请求数据不能为空"));
        }
        
        // 验证输入参数
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户名和密码不能为空"));
        }
        
        try {
            // 使用 UserService 进行用户验证
            User user = userService.login(username, password);
            
            if (user != null) {
                System.out.println("找到用户，用户名: " + username);
                
                // 生成JWT令牌
                String token = jwtUtils.generateToken(username);
                System.out.println("生成JWT令牌: " + token);
                
                // 将token保存到cookie中
                Cookie tokenCookie = new Cookie("token", token);
                tokenCookie.setPath("/");
                tokenCookie.setMaxAge(7200); // 2小时过期，与JWT token过期时间一致
                tokenCookie.setHttpOnly(true); // 防止XSS攻击
                tokenCookie.setSecure(false); // 开发环境使用HTTP，生产环境应设置为true
                // 使用正确的SameSite常量
                tokenCookie.setAttribute("SameSite", "Strict");
                response.addCookie(tokenCookie);
                
                // 获取用户菜单数据
                List<Menu> menusTree = menuService.getVisibleMenuTree();
                
                // 构建响应数据（不包含token、userId和tokenExpiration）
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("username", username);
                responseData.put("menus", menusTree);
                
                System.out.println("登录响应准备完成，即将返回");
                return ResponseEntity.ok(ApiResponse.success("登录成功", responseData));
            } else {
                System.out.println("密码验证失败，用户名: " + username);
            }
        } catch (Exception e) {
            // 记录异常日志
            e.printStackTrace();
        }
        
        // 登录失败，返回状态码和错误消息
        return ResponseEntity.badRequest().body(ApiResponse.error("用户名或密码错误"));
    }
    
    // 注册接口
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> registerRequest) {
        // 添加空值检查
        if (registerRequest == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("请求数据不能为空"));
        }
        
        // 从请求对象中获取用户名和密码
        String username = (String) registerRequest.get("username");
        String password = (String) registerRequest.get("password");
        
        // 基本验证
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty() || password.length() < 6) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户名不能为空，密码不能为空且长度不能少于6位"));
        }
        
        try {
            // 检查用户名是否已存在
            boolean usernameExists = false;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(
                         "SELECT * FROM users WHERE username = ?")) {
                
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                usernameExists = rs.next();
            }
            
            if (usernameExists) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户名已存在"));
            }
            
            // 密码加密
            String encodedPassword = passwordEncoder.encode(password);
            
            // 创建新用户
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(
                         "INSERT INTO users (username, password, roleId, roleName) VALUES (?, ?, ?, ?)",
                         PreparedStatement.RETURN_GENERATED_KEYS)) {
                
                stmt.setString(1, username);
                stmt.setString(2, encodedPassword);
                stmt.setLong(3, 2L); // 默认普通用户角色ID，与管理员的1L保持一致
                stmt.setString(4, "普通用户"); // 默认角色名称
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows == 0) {
                    return ResponseEntity.status(500).body(ApiResponse.error("注册失败，无法创建用户"));
                }
                
                // 获取生成的用户ID
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                Long userId = null;
                if (generatedKeys.next()) {
                    userId = generatedKeys.getLong(1);
                }
                
                // 构建响应数据
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("userId", userId);
                responseData.put("username", username);
                responseData.put("roleId", 2L);
                responseData.put("roleName", "普通用户");
                
                return ResponseEntity.ok(ApiResponse.success("注册成功", responseData));
            }
        } catch (Exception e) {
            // 记录异常日志
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("注册失败，请稍后重试"));
        }
    }
    
    // 密码重置接口
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, Object> resetPasswordRequest) {
        // 添加空值检查
        if (resetPasswordRequest == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("请求数据不能为空"));
        }
        
        // 从请求对象中获取用户名和新密码
        String username = (String) resetPasswordRequest.get("username");
        String newPassword = (String) resetPasswordRequest.get("newPassword");
        
        // 基本验证
        if (username == null || username.trim().isEmpty() || newPassword == null || newPassword.trim().isEmpty() || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户名不能为空，新密码不能为空且长度不能少于6位"));
        }
        
        try {
            // 检查用户是否存在
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(
                         "SELECT * FROM users WHERE username = ?")) {
                
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                
                if (!rs.next()) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("用户名不存在"));
                }
            }
            
            // 对新密码进行加密
            String encodedPassword = passwordEncoder.encode(newPassword);
            
            // 更新用户密码
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(
                         "UPDATE users SET password = ? WHERE username = ?")) {
                
                stmt.setString(1, encodedPassword);
                stmt.setString(2, username);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    // 密码重置成功
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("username", username);
                    
                    return ResponseEntity.ok(ApiResponse.success("密码重置成功", responseData));
                } else {
                    return ResponseEntity.status(500).body(ApiResponse.error("密码重置失败"));
                }
            }
        } catch (Exception e) {
            // 记录异常日志
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("密码重置失败，请稍后重试"));
        }
    }
}