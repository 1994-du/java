package com.springbootproject.Controller;

import com.springbootproject.Entity.User;
import com.springbootproject.Model.ApiResponse;
import com.springbootproject.Service.ChatMessageService;
import com.springbootproject.Service.UserService;
import com.springbootproject.Util.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    private Long getUserIdFromRequest(HttpServletRequest request) {
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
        if (token == null) {
            throw new RuntimeException("未登录");
        }
        String username = jwtUtils.getUsernameFromToken(token);
        if (username == null) {
            throw new RuntimeException("token无效");
        }
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user.getId();
    }

    @PostMapping("/history")
    public ResponseEntity<?> getChatHistory(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        try {
            Long userId = getUserIdFromRequest(request);
            
            System.out.println("=== 获取聊天记录接口调用 ===");
            System.out.println("=== 当前用户ID: " + userId + " ===");
            System.out.println("=== 请求参数: " + body + " ===");
            
            if (!body.containsKey("friendId")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("缺少好友ID"));
            }
            
            Long friendId = Long.parseLong(body.get("friendId").toString());
            int page = body.containsKey("page") ? Integer.parseInt(body.get("page").toString()) : 1;
            int size = body.containsKey("pageSize") ? Integer.parseInt(body.get("pageSize").toString()) : 
                       (body.containsKey("size") ? Integer.parseInt(body.get("size").toString()) : 50);
            
            if (page > 0) {
                page = page - 1;
            }
            
            System.out.println("=== 查询参数 - userId: " + userId + ", friendId: " + friendId + ", page: " + page + ", size: " + size + " ===");
            
            Map<String, Object> history = chatMessageService.getRecentChatHistory(userId, friendId, page, size);
            
            System.out.println("=== 查询结果数量: " + (history.containsKey("content") ? ((List<?>)history.get("content")).size() : 0) + " ===");
            
            return ResponseEntity.ok(ApiResponse.success("获取聊天记录成功", history));
        } catch (RuntimeException e) {
            System.out.println("=== 获取聊天记录失败: " + e.getMessage() + " ===");
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.out.println("=== 获取聊天记录异常: " + e.getMessage() + " ===");
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取聊天记录失败: " + e.getMessage()));
        }
    }

    @PostMapping("/groupHistory")
    public ResponseEntity<?> getGroupChatHistory(HttpServletRequest request) {
        try {
            List<Map<String, Object>> history = chatMessageService.getGroupChatHistory();
            return ResponseEntity.ok(ApiResponse.success("获取群聊记录成功", history));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取群聊记录失败: " + e.getMessage()));
        }
    }

    @PostMapping("/list")
    public ResponseEntity<?> getRecentChatFriends(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            List<Map<String, Object>> friends = chatMessageService.getRecentChatFriends(userId);
            return ResponseEntity.ok(ApiResponse.success("获取最近聊天好友成功", friends));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取最近聊天好友失败: " + e.getMessage()));
        }
    }
}
