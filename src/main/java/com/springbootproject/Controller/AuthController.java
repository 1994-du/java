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
import jakarta.servlet.http.HttpServletRequest;
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
                
                // 根据用户角色ID获取菜单数据
                List<Menu> menusTree = menuService.getMenusByRoleId(user.getRoleId());
                System.out.println("获取到的菜单树: " + menusTree);
                
                // 构建响应数据（包含token和头像地址）
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("username", username);
                responseData.put("menus", menusTree);
                responseData.put("token", token); // 添加token字段
                responseData.put("avatar", user.getAvatar()); // 添加用户头像地址
                
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
        
        // 从请求对象中获取用户名、密码和性别
        String username = (String) registerRequest.get("username");
        String password = (String) registerRequest.get("password");
        String gender = (String) registerRequest.get("gender");
        
        // 基本验证
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty() || password.length() < 6) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户名不能为空，密码不能为空且长度不能少于6位"));
        }
        
        try {
            // 使用UserService进行注册
            User newUser = userService.register(username, password, gender);
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("userId", newUser.getId());
            responseData.put("username", newUser.getUsername());
            responseData.put("roleId", newUser.getRoleId());
            responseData.put("roleName", newUser.getRoleName());
            
            return ResponseEntity.ok(ApiResponse.success("注册成功", responseData));
        } catch (Exception e) {
            // 记录异常日志
            e.printStackTrace();
            if (e.getMessage().contains("用户名已存在")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户名已存在"));
            }
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
            // 使用UserService进行密码重置
            User updatedUser = userService.resetPassword(username, newPassword);
            
            // 密码重置成功
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("username", updatedUser.getUsername());
            
            return ResponseEntity.ok(ApiResponse.success("密码重置成功", responseData));
        } catch (Exception e) {
            // 记录异常日志
            e.printStackTrace();
            if (e.getMessage().contains("用户名不存在")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户名不存在"));
            }
            return ResponseEntity.status(500).body(ApiResponse.error("密码重置失败，请稍后重试"));
        }
    }
    // 退出登录接口
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // 创建一个同名的cookie，但将其过期时间设置为0，这样浏览器会删除该cookie
        Cookie tokenCookie = new Cookie("token", null);
        tokenCookie.setPath("/");
        tokenCookie.setMaxAge(0); // 设置cookie立即过期
        tokenCookie.setHttpOnly(true);
        tokenCookie.setSecure(false);
        tokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(tokenCookie);
        
        // 返回退出成功响应
        return ResponseEntity.ok(ApiResponse.success("退出登录成功", null));
    }
    
    // 获取当前用户菜单接口
    @PostMapping("/getMenus")
    public ResponseEntity<?> getMenus(HttpServletRequest request) {
        try {
            // 从cookie中获取token
            String token = null;
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("token".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
            
            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("未登录或token已过期"));
            }
            
            // 解析token获取用户名
            String username = jwtUtils.getUsernameFromToken(token);
            if (username == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("token无效"));
            }
            
            // 获取用户信息
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            // 根据用户角色ID获取菜单数据
            List<Menu> menusTree = menuService.getMenusByRoleId(user.getRoleId());
            
            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("menus", menusTree);
            responseData.put("username", user.getUsername());
            responseData.put("avatar", user.getAvatar());
            
            return ResponseEntity.ok(ApiResponse.success("获取菜单成功", responseData));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取菜单失败: " + e.getMessage()));
        }
    }
}