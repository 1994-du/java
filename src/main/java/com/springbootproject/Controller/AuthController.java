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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private MenuService menuService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> loginRequest, HttpServletResponse response) {
        String username = (String) loginRequest.get("username");
        String password = (String) loginRequest.get("password");

        if (loginRequest == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("请求数据不能为空"));
        }

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户名和密码不能为空"));
        }

        try {
            User user = userService.login(username, password);

            if (user != null) {
                String token = jwtUtils.generateToken(username);

                Cookie tokenCookie = new Cookie("token", token);
                tokenCookie.setPath("/");
                tokenCookie.setMaxAge(7200);
                tokenCookie.setHttpOnly(true);
                tokenCookie.setSecure(false);
                tokenCookie.setAttribute("SameSite", "Strict");
                response.addCookie(tokenCookie);

                List<Menu> menusTree = menuService.getMenusByRoleId(user.getRoleId());

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("userId", user.getId());
                responseData.put("username", username);
                responseData.put("menus", menusTree);
                responseData.put("token", token);
                responseData.put("avatar", user.getAvatar());
                responseData.put("roleId", user.getRoleId());
                responseData.put("roleName", user.getRoleName());
                responseData.put("gender", user.getGender());

                return ResponseEntity.ok(ApiResponse.success("登录成功", responseData));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.badRequest().body(ApiResponse.error("用户名或密码错误"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> registerRequest) {
        if (registerRequest == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("请求数据不能为空"));
        }

        String username = (String) registerRequest.get("username");
        String password = (String) registerRequest.get("password");
        String gender = (String) registerRequest.get("gender");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty() || password.length() < 6) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户名不能为空，密码不能为空且长度不能少于6位"));
        }

        try {
            User newUser = userService.register(username, password, gender);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("userId", newUser.getId());
            responseData.put("username", newUser.getUsername());
            responseData.put("roleId", newUser.getRoleId());
            responseData.put("roleName", newUser.getRoleName());

            return ResponseEntity.ok(ApiResponse.success("注册成功", responseData));
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() != null && e.getMessage().contains("用户名已存在")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户名已存在"));
            }
            return ResponseEntity.status(500).body(ApiResponse.error("注册失败，请稍后重试"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, Object> resetPasswordRequest) {
        if (resetPasswordRequest == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("请求数据不能为空"));
        }

        String username = (String) resetPasswordRequest.get("username");
        String newPassword = (String) resetPasswordRequest.get("newPassword");

        if (username == null || username.trim().isEmpty() || newPassword == null || newPassword.trim().isEmpty() || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户名不能为空，新密码不能为空且长度不能少于6位"));
        }

        try {
            User updatedUser = userService.resetPassword(username, newPassword);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("username", updatedUser.getUsername());

            return ResponseEntity.ok(ApiResponse.success("密码重置成功", responseData));
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() != null && e.getMessage().contains("用户名不存在")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户名不存在"));
            }
            return ResponseEntity.status(500).body(ApiResponse.error("密码重置失败，请稍后重试"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie tokenCookie = new Cookie("token", null);
        tokenCookie.setPath("/");
        tokenCookie.setMaxAge(0);
        tokenCookie.setHttpOnly(true);
        tokenCookie.setSecure(false);
        tokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(tokenCookie);

        return ResponseEntity.ok(ApiResponse.success("退出登录成功", null));
    }
}