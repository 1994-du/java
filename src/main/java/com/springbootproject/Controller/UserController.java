package com.springbootproject.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import com.springbootproject.Entity.User;
import com.springbootproject.Service.UserService;
import com.springbootproject.Model.AddUserRequest;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
@RestController  // 标记为 REST 控制器
@RequestMapping("/api/users")
public class UserController {
    
    // 测试认证状态的端点
    @GetMapping("/test-auth")
    public ResponseEntity<Map<String, Object>> testAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && 
                                 !(authentication instanceof AnonymousAuthenticationToken);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", 200);
        response.put("message", "认证状态测试");
        response.put("isAuthenticated", isAuthenticated);
        response.put("authentication", authentication != null ? authentication.toString() : "null");
        response.put("username", isAuthenticated ? authentication.getName() : "未认证");
        
        System.out.println("测试认证状态 - 已认证: " + isAuthenticated + ", 用户: " + 
                          (isAuthenticated ? authentication.getName() : "未认证"));
        
        return ResponseEntity.ok(response);
    }

    @Autowired  // 自动注入 UserService
    private UserService userService;

    // 通过用户名获取用户信息（需要JWT认证）
    @GetMapping("/{username}")
    public ResponseEntity<Map<String, Object>> getUserByUsername(@PathVariable String username) {
        User user = userService.findUserByUsername(username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", 200);
        response.put("message", "获取用户信息成功");
        response.put("data", user);
        
        return ResponseEntity.ok(response);
    }

    // 注：create接口已被addUser接口替代，addUser接口功能更完整，支持头像和角色设置
    
    /**
     * 获取当前登录用户信息（需要JWT认证）
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        // 从SecurityContext中获取当前认证的用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        String username = authentication.getName();
        
        // 构建响应
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", 200);
        response.put("message", "获取当前用户信息成功");
        response.put("username", username);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取所有用户信息（支持分页）
     * 同时支持GET和POST请求，以兼容前端不同的请求方式
     * 注意：前端传入的page参数是从1开始的，而Spring Data JPA使用从0开始的页码
     */
    @RequestMapping(value = "/all", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestBody(required = false) Map<String, Object> requestBody) {
        
        // 如果是POST请求且有请求体，优先从请求体获取分页参数
        if (requestBody != null) {
            // 尝试从请求体获取page参数
            if (requestBody.containsKey("page")) {
                Object pageObj = requestBody.get("page");
                try {
                    if (pageObj instanceof Number) {
                        page = ((Number) pageObj).intValue();
                    } else if (pageObj instanceof String) {
                        page = Integer.parseInt((String) pageObj);
                    }
                } catch (Exception e) {
                    System.err.println("解析page参数失败: " + e.getMessage());
                }
            }
            
            // 尝试从请求体获取pageSize参数
            if (requestBody.containsKey("pageSize")) {
                Object pageSizeObj = requestBody.get("pageSize");
                try {
                    if (pageSizeObj instanceof Number) {
                        pageSize = ((Number) pageSizeObj).intValue();
                    } else if (pageSizeObj instanceof String) {
                        pageSize = Integer.parseInt((String) pageSizeObj);
                    }
                } catch (Exception e) {
                    System.err.println("解析pageSize参数失败: " + e.getMessage());
                }
            }
        }
        
        // 确保page和pageSize为有效值
        if (page < 1) page = 1; // 前端页码从1开始
        if (pageSize < 1 || pageSize > 100) pageSize = 10;
        
        // 创建分页对象，将前端页码转换为JPA需要的从0开始的页码
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        
        // 获取分页用户数据
        Page<User> userPage = userService.getAllUsers(pageable);
        
        // 过滤用户列表，移除password字段
        List<Map<String, Object>> userListWithoutPassword = new ArrayList<>();
        for (User user : userPage.getContent()) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("avatar", user.getAvatar());
            userMap.put("roleId", user.getRoleId());
            userMap.put("roleName", user.getRoleName());
            userListWithoutPassword.add(userMap);
        }
        
        // 构建响应，返回前端需要的从1开始的页码
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", 200);
        response.put("message", "获取用户列表成功");
        response.put("data", userListWithoutPassword);
        response.put("total", userPage.getTotalElements());
        response.put("page", page); // 返回前端原始传入的页码
        response.put("pageSize", userPage.getSize());
        response.put("totalPages", userPage.getTotalPages());
        response.put("hasNext", userPage.hasNext());
        response.put("hasPrevious", userPage.hasPrevious());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 添加用户（包含头像和角色）
     * 使用JSON格式的body传参，支持Base64编码的头像数据
     * 需要JWT认证
     */
    @PostMapping("/addUser")
    public ResponseEntity<Map<String, Object>> addUser(@RequestBody AddUserRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        String avatarUrl = null;
        
        try {
            // 记录请求参数
            System.out.println("收到addUser请求，用户名: " + request.getUsername() + ", roleId: " + request.getRoleId());
            
            // 验证必要参数
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                throw new RuntimeException("用户名不能为空");
            }
            
            // 直接使用前端传来的avatar字符串，不进行处理
            if (request.getAvatar() != null && !request.getAvatar().trim().isEmpty()) {
                avatarUrl = request.getAvatar();
            }
            
            // 调用服务层创建用户
            // 不传递密码，系统将生成默认密码或使用其他认证方式
            User newUser = userService.createUserWithAvatarAndRole(
                    request.getUsername(), 
                    null, // 密码参数为null
                    avatarUrl, 
                    request.getRoleId());
            
            // 记录创建的用户信息
            System.out.println("用户创建成功，ID: " + newUser.getId() + ", 角色ID: " + newUser.getRoleId() + ", 角色名称: " + newUser.getRoleName());
            
            response.put("success", true);
            response.put("status", 200);
            response.put("message", "用户创建成功");
            response.put("data", newUser);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("status", 400);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("status", 500);
            response.put("message", "内部服务器错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 重置用户密码接口
     * 接收JSON格式参数，包含id字段，将用户密码重置为默认的"123456"（加密）
     */
    @PostMapping("/resetPassword")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证请求体是否包含id字段
            if (!requestBody.containsKey("id") || requestBody.get("id") == null) {
                response.put("success", false);
                response.put("status", 400);
                response.put("message", "用户ID不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 提取用户ID
            Long userId = Long.parseLong(requestBody.get("id").toString());
            
            // 调用UserService重置密码
            User updatedUser = userService.resetPassword(userId);
            
            // 构建响应数据
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", updatedUser.getId());
            userData.put("username", updatedUser.getUsername());
            userData.put("avatar", updatedUser.getAvatar());
            userData.put("roleId", updatedUser.getRoleId());
            userData.put("roleName", updatedUser.getRoleName());
            
            response.put("success", true);
            response.put("status", 200);
            response.put("message", "密码重置成功，默认密码为：123456");
            response.put("data", userData);
            
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("status", 400);
            response.put("message", "用户ID格式错误");
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("status", 404);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("status", 500);
            response.put("message", "密码重置失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 根据ID删除用户
     * 需要JWT认证，通过JSON请求体传递用户ID
     */
    @PostMapping("/deleteUser")
    public ResponseEntity<Map<String, Object>> deleteUser(@RequestBody Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取用户ID参数
            Object idObj = requestBody.get("id");
            if (idObj == null) {
                throw new RuntimeException("用户ID不能为空");
            }
            
            // 转换ID为Long类型
            Long userId;
            if (idObj instanceof Number) {
                userId = ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                userId = Long.parseLong((String) idObj);
            } else {
                throw new RuntimeException("用户ID格式不正确");
            }
            
            // 记录删除请求
            System.out.println("收到删除用户请求，用户ID: " + userId);
            
            // 调用服务层删除用户
            userService.deleteUserById(userId);
            
            response.put("success", true);
            response.put("status", 200);
            response.put("message", "用户删除成功");
            
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("status", 400);
            response.put("message", "用户ID必须是有效的数字");
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("status", 400);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("status", 500);
            response.put("message", "内部服务器错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 上传文件并返回地址
     * 简化版本：只上传文件并返回访问地址，不进行用户关联和其他校验
     */
    @PostMapping("/updateAvatar")
    public ResponseEntity<Map<String, Object>> updateAvatar(@RequestPart("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证文件是否为空
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("status", 400);
                response.put("message", "上传的文件不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 确保上传目录存在
            String uploadDir = "uploads/avatars/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String filename = UUID.randomUUID().toString() + fileExtension;
            
            // 保存文件
            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, file.getBytes());
            
            // 构建文件URL
            String avatarUrl = "/uploads/avatars/" + filename;
            
            response.put("success", true);
            response.put("status", 200);
            response.put("message", "文件上传成功");
            response.put("avatarUrl", avatarUrl);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("status", 500);
            response.put("message", "文件保存失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("status", 500);
            response.put("message", "文件上传失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 更新用户信息接口
     * 接收JSON格式参数，包含id、avatar、username、roleId等字段
     */
    @PostMapping("/setUser")
    public ResponseEntity<Map<String, Object>> setUser(@RequestBody Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证请求体是否包含id字段
            if (!requestBody.containsKey("id") || requestBody.get("id") == null) {
                response.put("success", false);
                response.put("status", 400);
                response.put("message", "用户ID不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 提取参数
            Long id = Long.parseLong(requestBody.get("id").toString());
            String avatar = requestBody.containsKey("avatar") ? (String) requestBody.get("avatar") : null;
            String username = requestBody.containsKey("username") ? (String) requestBody.get("username") : null;
            Long roleId = null;
            if (requestBody.containsKey("roleId") && requestBody.get("roleId") != null) {
                roleId = Long.parseLong(requestBody.get("roleId").toString());
            }
            
            // 调用UserService更新用户信息
            User updatedUser = userService.updateUser(id, username, avatar, roleId);
            
            // 构建响应数据，不包含密码
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", updatedUser.getId());
            userData.put("username", updatedUser.getUsername());
            userData.put("avatar", updatedUser.getAvatar());
            userData.put("roleId", updatedUser.getRoleId());
            userData.put("roleName", updatedUser.getRoleName());
            
            response.put("success", true);
            response.put("status", 200);
            response.put("message", "用户信息更新成功");
            response.put("data", userData);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("status", 400);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("status", 500);
            response.put("message", "更新用户信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}