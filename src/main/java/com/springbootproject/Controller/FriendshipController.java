package com.springbootproject.Controller;

import com.springbootproject.Entity.Friendship;
import com.springbootproject.Entity.User;
import com.springbootproject.Model.ApiResponse;
import com.springbootproject.Service.FriendshipService;
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
@RequestMapping("/api/friend")
public class FriendshipController {

    @Autowired
    private FriendshipService friendshipService;

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

    @PostMapping("/add")
    public ResponseEntity<?> sendFriendRequest(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        try {
            Long userId = getUserIdFromRequest(request);
            Long friendId = Long.parseLong(body.get("friendId").toString());
            Friendship friendship = friendshipService.sendFriendRequest(userId, friendId);
            return ResponseEntity.ok(ApiResponse.success("好友请求已发送", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("发送好友请求失败: " + e.getMessage()));
        }
    }

    @PostMapping("/accept")
    public ResponseEntity<?> acceptFriendRequest(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        try {
            Long userId = getUserIdFromRequest(request);
            Long friendshipId = Long.parseLong(body.get("friendshipId").toString());
            Friendship friendship = friendshipService.acceptFriendRequest(friendshipId, userId);
            return ResponseEntity.ok(ApiResponse.success("已接受好友请求", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("接受好友请求失败: " + e.getMessage()));
        }
    }

    @PostMapping("/reject")
    public ResponseEntity<?> rejectFriendRequest(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        try {
            Long userId = getUserIdFromRequest(request);
            Long friendshipId = Long.parseLong(body.get("friendshipId").toString());
            Friendship friendship = friendshipService.rejectFriendRequest(friendshipId, userId);
            return ResponseEntity.ok(ApiResponse.success("已拒绝好友请求", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("拒绝好友请求失败: " + e.getMessage()));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteFriend(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        try {
            Long userId = getUserIdFromRequest(request);
            Long friendId = Long.parseLong(body.get("friendId").toString());
            friendshipService.deleteFriend(userId, friendId);
            return ResponseEntity.ok(ApiResponse.success("已删除好友", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("删除好友失败: " + e.getMessage()));
        }
    }

    @PostMapping("/list")
    public ResponseEntity<?> getFriendList(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            List<Map<String, Object>> friends = friendshipService.getFriendList(userId);
            return ResponseEntity.ok(ApiResponse.success("获取好友列表成功", friends));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取好友列表失败: " + e.getMessage()));
        }
    }

    @PostMapping("/requests/pending")
    public ResponseEntity<?> getPendingRequests(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            List<Map<String, Object>> requests = friendshipService.getPendingRequests(userId);
            return ResponseEntity.ok(ApiResponse.success("获取待处理好友请求成功", requests));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取待处理好友请求失败: " + e.getMessage()));
        }
    }

    @PostMapping("/requests/sent")
    public ResponseEntity<?> getSentRequests(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            List<Map<String, Object>> requests = friendshipService.getSentRequests(userId);
            return ResponseEntity.ok(ApiResponse.success("获取已发送好友请求成功", requests));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取已发送好友请求失败: " + e.getMessage()));
        }
    }

    @PostMapping("/check")
    public ResponseEntity<?> checkFriendship(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        try {
            Long userId = getUserIdFromRequest(request);
            Long friendId = Long.parseLong(body.get("friendId").toString());
            boolean areFriends = friendshipService.areFriends(userId, friendId);
            return ResponseEntity.ok(ApiResponse.success("查询成功", areFriends));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("查询好友关系失败: " + e.getMessage()));
        }
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchUsers(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        try {
            Long userId = getUserIdFromRequest(request);
            String keyword = body.get("keyword") != null ? body.get("keyword").toString() : "";
            List<Map<String, Object>> users = friendshipService.searchUsers(userId, keyword);
            return ResponseEntity.ok(ApiResponse.success("搜索用户成功", users));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("搜索用户失败: " + e.getMessage()));
        }
    }

    @PostMapping("/recommend")
    public ResponseEntity<?> getRecommendedFriends(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            List<Map<String, Object>> users = friendshipService.getRecommendedFriends(userId);
            return ResponseEntity.ok(ApiResponse.success("获取推荐好友成功", users));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取推荐好友失败: " + e.getMessage()));
        }
    }
}
