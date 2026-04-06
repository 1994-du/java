package com.springbootproject.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springbootproject.Config.WebSocketConfig;
import com.springbootproject.Entity.ChatMessage;
import com.springbootproject.Entity.User;
import com.springbootproject.Service.ChatMessageService;
import com.springbootproject.Service.UserService;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ServerEndpoint(value = "/ws", configurator = WebSocketConfig.class)
public class NativeWebSocketController {
    
    private static UserService userService;
    private static ChatMessageService chatMessageService;
    
    @Autowired
    public void setUserService(UserService userService) {
        NativeWebSocketController.userService = userService;
    }
    
    @Autowired
    public void setChatMessageService(ChatMessageService chatMessageService) {
        NativeWebSocketController.chatMessageService = chatMessageService;
    }
    
    private static final CopyOnWriteArraySet<Session> webSocketSet = new CopyOnWriteArraySet<>();
    private static final AtomicInteger onlineCount = new AtomicInteger(0);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ConcurrentHashMap<Long, Map<String, Object>> onlineUsers = new ConcurrentHashMap<>();
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("=== WebSocket新连接加入 ===");
        if (session != null) {
            webSocketSet.add(session);
            onlineCount.incrementAndGet();
            System.out.println("=== WebSocket在线人数增加为: " + onlineCount.get() + " ===");
            
            sendOnlineUsersList(session);
        }
    }
    
    private void sendOnlineUsersList(Session session) {
        try {
            List<Map<String, Object>> usersList = new ArrayList<>();
            for (Map<String, Object> userInfo : onlineUsers.values()) {
                Map<String, Object> userWithAvatar = new HashMap<>(userInfo);
                
                if (userInfo.containsKey("userId") && userService != null) {
                    try {
                        Long uid = Long.parseLong(userInfo.get("userId").toString());
                        User user = userService.findUserById(uid);
                        if (user != null && user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                            String avatar = user.getAvatar();
                            if (avatar.startsWith("/uploads/")) {
                                userWithAvatar.put("avatar", avatar);
                            } else {
                                userWithAvatar.put("avatar", "/uploads/avatars/" + avatar);
                            }
                        } else {
                            userWithAvatar.put("avatar", "/uploads/avatars/default.png");
                        }
                    } catch (Exception e) {
                        userWithAvatar.put("avatar", "/uploads/avatars/default.png");
                    }
                } else {
                    userWithAvatar.put("avatar", "/uploads/avatars/default.png");
                }
                
                usersList.add(userWithAvatar);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "onlineUsers");
            response.put("payload", usersList);
            
            String json = objectMapper.writeValueAsString(response);
            session.getBasicRemote().sendText(json);
            System.out.println("=== 发送在线用户列表，数量: " + usersList.size() + " ===");
            
            sendRecentChatHistory(session);
        } catch (Exception e) {
            System.out.println("=== 发送在线用户列表失败: " + e.getMessage() + " ===");
        }
    }
    
    private void sendRecentChatHistory(Session session) {
        try {
            if (chatMessageService == null) {
                return;
            }
            
            List<ChatMessage> messages = chatMessageService.getRecentMessages();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            List<Map<String, Object>> historyList = new ArrayList<>();
            for (ChatMessage msg : messages) {
                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("id", msg.getId());
                msgMap.put("userId", msg.getUserId());
                msgMap.put("username", msg.getUsername());
                msgMap.put("message", msg.getMessage());
                msgMap.put("avatar", msg.getAvatar() != null ? msg.getAvatar() : "/uploads/avatars/default.png");
                msgMap.put("time", msg.getCreateTime() != null ? sdf.format(java.sql.Timestamp.valueOf(msg.getCreateTime())) : "");
                historyList.add(msgMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "chatHistory");
            response.put("payload", historyList);
            
            String json = objectMapper.writeValueAsString(response);
            session.getBasicRemote().sendText(json);
            System.out.println("=== 发送最近聊天记录，数量: " + historyList.size() + " ===");
        } catch (Exception e) {
            System.out.println("=== 发送聊天记录失败: " + e.getMessage() + " ===");
        }
    }
    
    @OnClose
    public void onClose(Session session) {
        System.out.println("连接关闭");
        
        if (session != null) {
            webSocketSet.remove(session);
            onlineCount.decrementAndGet();
            System.out.println("在线人数减少为: " + onlineCount.get());
        }
    }
    
    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("[消息接收] 收到客户端消息，长度: " + message.length());
        
        try {
            if (message.length() > 1024 * 1024 * 5) {
                System.out.println("[消息拒绝] 消息过大，发送错误提示");
                sendSimpleErrorMessage(session, "消息过大，最大支持5MB");
                return;
            }
            
            Map<String, Object> messageMap = null;
            try {
                messageMap = objectMapper.readValue(message, Map.class);
                System.out.println("[JSON解析] 成功解析JSON消息");
            } catch (Exception e) {
                System.out.println("[JSON解析失败] 消息不是有效的JSON格式: " + e.getMessage());
                sendSimpleErrorMessage(session, "消息格式错误，请发送有效的JSON格式");
                return;
            }
            
            System.out.println("[消息验证] 接收到的messageMap: " + messageMap);
            if (messageMap == null || !messageMap.containsKey("type")) {
                System.out.println("[消息验证] 消息缺少type字段，messageMap内容: " + messageMap);
                sendSimpleErrorMessage(session, "消息缺少必要的type字段");
                return;
            }
            
            String messageType = messageMap.get("type").toString();
            System.out.println("[消息类型] 处理消息类型: " + messageType);
            
            switch (messageType) {
                case "userOnline":
                    handleUserOnline(messageMap, session);
                    break;
                case "userOffline":
                    handleUserOffline(messageMap, session);
                    break;
                case "chat":
                    handleChatMessage(messageMap, session);
                    break;
                default:
                    System.out.println("[消息类型] 未知消息类型: " + messageType);
                    sendSimpleErrorMessage(session, "不支持的消息类型");
            }
            
        } catch (Throwable t) {
            System.out.println("[致命异常] 处理消息时发生严重错误: " + t.getMessage());
        }
        
        System.out.println("[消息处理完成] 消息处理流程结束");
    }
    
    private void handleUserOnline(Map<String, Object> messageMap, Session session) {
        System.out.println("=== 开始处理用户上线消息 ===");
        try {
            if (!messageMap.containsKey("payload")) {
                System.out.println("=== 错误：用户上线消息缺少payload字段 ===");
                sendSimpleErrorMessage(session, "用户上线消息缺少payload字段");
                return;
            }
            
            Map<String, Object> payload = (Map<String, Object>) messageMap.get("payload");
            String username = payload.getOrDefault("username", "匿名用户").toString();
            Long userId = null;
            if (payload.containsKey("userId")) {
                try {
                    userId = Long.parseLong(payload.get("userId").toString());
                } catch (NumberFormatException e) {
                    System.out.println("=== userId格式错误 ===");
                }
            }
            System.out.println("=== 接收到用户名: " + username + ", userId: " + userId + " ===");
            
            if (session != null) {
                session.getUserProperties().put("username", username);
                if (userId != null) {
                    session.getUserProperties().put("userId", userId);
                }
                System.out.println("=== 成功设置用户属性: " + username + " ===");
            }
            
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            
            Map<String, Object> responsePayload = new HashMap<>();
            if (userId != null) {
                responsePayload.put("userId", userId);
                
                Map<String, Object> onlineUserInfo = new HashMap<>();
                onlineUserInfo.put("userId", userId);
                onlineUserInfo.put("username", username);
                onlineUserInfo.put("time", time);
                onlineUsers.put(userId, onlineUserInfo);
            }
            responsePayload.put("username", username);
            responsePayload.put("time", time);
            responsePayload.put("onlineCount", onlineCount.get());
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "userOnline");
            response.put("payload", responsePayload);
            
            String broadcastJson = objectMapper.writeValueAsString(response);
            System.out.println("=== 用户上线消息JSON: " + broadcastJson + " ===");
            
            broadcastMessage(broadcastJson);
            
            System.out.println("=== 用户上线处理完成 ===");
            
        } catch (Exception e) {
            System.out.println("=== 处理用户上线时发生异常: " + e.getMessage() + " ===");
            e.printStackTrace();
            sendSimpleErrorMessage(session, "处理用户上线失败: " + e.getMessage());
        }
    }
    
    private void handleUserOffline(Map<String, Object> messageMap, Session session) {
        System.out.println("=== 开始处理用户下线消息 ===");
        try {
            if (!messageMap.containsKey("payload")) {
                System.out.println("=== 错误：用户下线消息缺少payload字段 ===");
                sendSimpleErrorMessage(session, "用户下线消息缺少payload字段");
                return;
            }
            
            Map<String, Object> payload = (Map<String, Object>) messageMap.get("payload");
            String username = payload.getOrDefault("username", "匿名用户").toString();
            
            Long userId = null;
            if (payload.containsKey("userId")) {
                try {
                    userId = Long.parseLong(payload.get("userId").toString());
                } catch (NumberFormatException e) {
                    System.out.println("=== userId格式错误 ===");
                }
            }
            System.out.println("=== 接收到用户名: " + username + ", userId: " + userId + " ===");
            
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            
            Map<String, Object> responsePayload = new HashMap<>();
            if (userId != null) {
                responsePayload.put("userId", userId);
                onlineUsers.remove(userId);
            }
            responsePayload.put("username", username);
            responsePayload.put("time", time);
            responsePayload.put("onlineCount", Math.max(0, onlineCount.get() - 1));
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "userOffline");
            response.put("payload", responsePayload);
            
            String broadcastJson = objectMapper.writeValueAsString(response);
            System.out.println("=== 用户下线消息JSON: " + broadcastJson + " ===");
            
            broadcastMessage(broadcastJson);
            
            System.out.println("=== 用户下线处理完成 ===");
            
        } catch (Exception e) {
            System.out.println("=== 处理用户下线时发生异常: " + e.getMessage() + " ===");
            e.printStackTrace();
            sendSimpleErrorMessage(session, "处理用户下线失败: " + e.getMessage());
        }
    }
    
    private void broadcastUserOffline(String username) {
        try {
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("username", username);
            payload.put("time", time);
            payload.put("onlineCount", onlineCount.get());
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "userOffline");
            response.put("payload", payload);
            
            String broadcastJson = objectMapper.writeValueAsString(response);
            System.out.println("=== 用户下线消息JSON: " + broadcastJson + " ===");
            
            broadcastMessage(broadcastJson);
            
        } catch (Exception e) {
            System.out.println("=== 广播用户下线失败: " + e.getMessage() + " ===");
        }
    }
    
    private void handleChatMessage(Map<String, Object> messageMap, Session session) {
        System.out.println("=== 开始处理聊天消息 ===");
        try {
            if (!messageMap.containsKey("payload")) {
                System.out.println("=== 错误：聊天消息缺少payload字段 ===");
                sendSimpleErrorMessage(session, "聊天消息缺少payload字段");
                return;
            }
            
            Map<String, Object> originalPayload = (Map<String, Object>) messageMap.get("payload");
            System.out.println("=== 收到原始payload，包含字段数: " + originalPayload.size() + " ===");
            
            String imageData = null;
            String imageFieldName = null;
            
            if (originalPayload.containsKey("isImage") && Boolean.TRUE.equals(originalPayload.get("isImage")) || 
                originalPayload.containsKey("imageBase64") ||
                originalPayload.containsKey("image")) {
                System.out.println("=== 检测到图片消息 ===");
                
                if (originalPayload.containsKey("image")) {
                    imageData = String.valueOf(originalPayload.get("image"));
                    imageFieldName = "image";
                } else if (originalPayload.containsKey("imageBase64")) {
                    imageData = String.valueOf(originalPayload.get("imageBase64"));
                    imageFieldName = "imageBase64";
                }
                
                if (imageData != null) {
                    System.out.println("=== 使用" + imageFieldName + "字段，数据长度: " + imageData.length() + " 字符 ===");
                }
            }
            
            Map<String, String> processedPayload = new HashMap<>();
            
            Long chatUserId = null;
            for (Map.Entry<String, Object> entry : originalPayload.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value != null) {
                    String stringValue = value.toString();
                    processedPayload.put(key, stringValue);
                    if ("userId".equals(key) || "fromUserId".equals(key)) {
                        try {
                            chatUserId = Long.parseLong(stringValue);
                        } catch (NumberFormatException e) {
                            System.out.println("=== userId格式错误 ===");
                        }
                    }
                }
            }
            
            if (chatUserId != null) {
                processedPayload.put("userId", chatUserId.toString());
            }
            
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            if (!processedPayload.containsKey("time")) {
                processedPayload.put("time", time);
            }
            
            if (!processedPayload.containsKey("username")) {
                String username = "匿名用户";
                if (session != null && session.getUserProperties().containsKey("username")) {
                    username = session.getUserProperties().get("username").toString();
                }
                processedPayload.put("username", username);
            }
            
            String avatarUrl = "/uploads/avatars/default.png";
            if (chatUserId != null) {
                System.out.println("=== 尝试获取用户头像，userId: " + chatUserId + " ===");
                if (userService != null) {
                    User user = userService.findUserById(chatUserId);
                    System.out.println("=== 查询到用户: " + (user != null ? user.getUsername() : "null") + " ===");
                    if (user != null) {
                        System.out.println("=== 用户头像: " + user.getAvatar() + " ===");
                        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                            String avatar = user.getAvatar();
                            if (avatar.startsWith("/uploads/")) {
                                avatarUrl = avatar;
                            } else {
                                avatarUrl = "/uploads/avatars/" + avatar;
                            }
                        }
                    }
                } else {
                    System.out.println("=== userService为null ===");
                }
            } else {
                System.out.println("=== chatUserId为null ===");
            }
            System.out.println("=== 最终头像URL: " + avatarUrl + " ===");
            
            processedPayload.put("avatar", avatarUrl);
            
            if (imageData != null) {
                try {
                    String imageUrl = saveBase64ImageExtremelySafe(imageData);
                    processedPayload.put("imageUrl", imageUrl);
                    processedPayload.put("image", imageData);
                } catch (Throwable t) {
                    System.out.println("=== 处理图片数据时发生异常: " + t.getMessage() + " ===");
                }
            }
            
            if (!processedPayload.containsKey("message") && originalPayload.containsKey("content")) {
                String content = originalPayload.get("content").toString();
                processedPayload.put("message", content);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "chat");
            response.put("payload", processedPayload);
            
            String chatJson = objectMapper.writeValueAsString(response);
            broadcastMessage(chatJson);
            
            if (chatMessageService != null && processedPayload.containsKey("message")) {
                try {
                    String username = processedPayload.getOrDefault("username", "匿名用户");
                    chatMessageService.saveMessage(chatUserId, username, processedPayload.get("message"), avatarUrl);
                } catch (Exception e) {
                    System.out.println("=== 保存聊天记录失败: " + e.getMessage() + " ===");
                }
            }
            
            System.out.println("=== 聊天消息广播完成 ===");
            
        } catch (Exception e) {
            System.out.println("=== 处理聊天消息时发生异常: " + e.getMessage() + " ===");
            e.printStackTrace();
            sendSimpleErrorMessage(session, "处理消息失败: " + e.getMessage());
        }
    }
    
    private void sendSimpleErrorMessage(Session session, String errorMessage) {
        try {
            if (session != null && session.isOpen()) {
                Map<String, Object> response = new HashMap<>();
                response.put("type", "error");
                Map<String, String> payload = new HashMap<>();
                payload.put("message", errorMessage);
                response.put("payload", payload);
                
                String errorJson = objectMapper.writeValueAsString(response);
                session.getBasicRemote().sendText(errorJson);
                System.out.println("[错误消息] 已发送错误提示: " + errorMessage);
            }
        } catch (Exception e) {
            System.out.println("[错误消息] 发送错误消息失败: " + e.getMessage());
        }
    }
    
    private void broadcastMessage(String message) {
        System.out.println("[广播开始] 准备广播消息，消息长度: " + message.length());
        
        if (message == null || message.isEmpty()) {
            return;
        }
        
        if (message.length() > 1024 * 1024 * 2) {
            message = message.substring(0, 1024 * 1024 * 2) + "...[消息过长已截断]";
        }
        
        if (webSocketSet == null || webSocketSet.isEmpty()) {
            return;
        }
        
        List<Session> sessionsToProcess = new ArrayList<>();
        try {
            sessionsToProcess.addAll(webSocketSet);
        } catch (Exception e) {
            return;
        }
        
        List<Session> sessionsToRemove = new ArrayList<>();
        
        for (Session session : sessionsToProcess) {
            try {
                if (session == null || !session.isOpen()) {
                    sessionsToRemove.add(session);
                    continue;
                }
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                if (session != null) {
                    sessionsToRemove.add(session);
                }
            }
        }
        
        if (!sessionsToRemove.isEmpty()) {
            for (Session sessionToRemove : sessionsToRemove) {
                try {
                    webSocketSet.remove(sessionToRemove);
                    if (sessionToRemove != null) {
                        sessionToRemove.close();
                    }
                } catch (Exception e) {
                    System.out.println("[广播] 移除无效会话失败: " + e.getMessage());
                }
            }
            onlineCount.set(Math.max(0, onlineCount.get() - sessionsToRemove.size()));
        }
        
        System.out.println("[广播结束] 消息广播完成");
    }
    
    private String saveBase64ImageExtremelySafe(String base64Data) {
        try {
            String shortUuid = UUID.randomUUID().toString().substring(0, 8);
            return "image_" + shortUuid + ".jpg";
        } catch (Throwable t) {
            return "safe_image.jpg";
        }
    }
}
