package com.springbootproject.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springbootproject.Config.WebSocketConfig;
import com.springbootproject.Entity.User;
import com.springbootproject.Service.ChatMessageService;
import com.springbootproject.Service.FriendshipService;
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
    private static FriendshipService friendshipService;
    private static ChatMessageService chatMessageService;
    
    @Autowired
    public void setUserService(UserService userService) {
        NativeWebSocketController.userService = userService;
    }
    
    @Autowired
    public void setFriendshipService(FriendshipService friendshipService) {
        NativeWebSocketController.friendshipService = friendshipService;
    }
    
    @Autowired
    public void setChatMessageService(ChatMessageService chatMessageService) {
        NativeWebSocketController.chatMessageService = chatMessageService;
    }
    
    private static final CopyOnWriteArraySet<Session> webSocketSet = new CopyOnWriteArraySet<>();
    private static final AtomicInteger onlineCount = new AtomicInteger(0);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ConcurrentHashMap<Long, Session> userSessionMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Session, Long> sessionUserMap = new ConcurrentHashMap<>();
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("=== WebSocket新连接加入 ===");
        if (session != null) {
            webSocketSet.add(session);
            onlineCount.incrementAndGet();
            System.out.println("=== WebSocket在线人数增加为: " + onlineCount.get() + " ===");
            try {
                String welcomeMessage = "{\"type\":\"system\",\"payload\":{\"message\":\"欢迎连接WebSocket服务器！\"}}";
                session.getBasicRemote().sendText(welcomeMessage);
                System.out.println("=== 已发送欢迎消息给新连接 ===");
            } catch (Exception e) {
                System.out.println("=== 发送欢迎消息失败: " + e.getMessage() + " ===");
            }
        }
    }
    
    @OnClose
    public void onClose(Session session) {
        System.out.println("连接关闭");
        
        if (session != null) {
            try {
                Long userId = sessionUserMap.remove(session);
                if (userId != null) {
                    userSessionMap.remove(userId);
                    System.out.println("=== 移除用户会话映射，用户ID: " + userId + " ===");
                }
                webSocketSet.remove(session);
                onlineCount.decrementAndGet();
                System.out.println("在线人数减少为: " + onlineCount.get());
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                case "userInfo":
                case "username":
                    handleUserInfoMessage(messageMap, session);
                    break;
                case "chat":
                    handleChatMessage(messageMap, session);
                    break;
                case "private":
                    handlePrivateMessage(messageMap, session);
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
    
    private void handleUserInfoMessage(Map<String, Object> messageMap, Session session) {
        System.out.println("=== 开始处理用户信息消息 ===");
        try {
            if (!messageMap.containsKey("payload")) {
                System.out.println("=== 错误：用户信息消息缺少payload字段 ===");
                sendSimpleErrorMessage(session, "用户信息消息缺少payload字段");
                return;
            }
            
            Map<String, Object> payload = (Map<String, Object>) messageMap.get("payload");
            System.out.println("=== 收到的payload内容: " + payload + " ===");
            System.out.println("=== payload中的所有key: " + payload.keySet() + " ===");
            String username = payload.getOrDefault("username", "匿名用户").toString();
            System.out.println("=== 接收到用户名: " + username + " ===");
            
            if (session != null) {
                session.getUserProperties().put("username", username);
                System.out.println("=== 成功设置用户属性: " + username + " ===");
            }
            
            if (payload.containsKey("userId")) {
                System.out.println("=== payload包含userId字段 ===");
                System.out.println("=== userId原始值: " + payload.get("userId") + " ===");
                System.out.println("=== userId类型: " + (payload.get("userId") != null ? payload.get("userId").getClass().getName() : "null") + " ===");
                try {
                    Long userId = Long.parseLong(payload.get("userId").toString());
                    userSessionMap.put(userId, session);
                    sessionUserMap.put(session, userId);
                    System.out.println("=== 建立用户ID与会话映射成功，userId: " + userId + " ===");
                } catch (NumberFormatException e) {
                    System.out.println("=== 用户ID格式错误，无法转换为Long: " + e.getMessage() + " ===");
                }
            } else {
                System.out.println("=== payload不包含userId字段 ===");
            }
            
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\"type\":\"userJoined\",\"payload\":{");
            jsonBuilder.append("\"username\":\"").append(escapeJson(username)).append("\",");
            jsonBuilder.append("\"time\":\"").append(escapeJson(time)).append("\",");
            jsonBuilder.append("\"message\":\"").append(escapeJson(username + " 加入了聊天室")).append("\",");
            jsonBuilder.append("\"onlineCount\":").append(onlineCount.get()).append(",");
            jsonBuilder.append("\"avatar\":\"/uploads/avatars/default.png\"");
            jsonBuilder.append("}}");
            
            String broadcastJson = jsonBuilder.toString();
            System.out.println("=== 用户加入消息JSON: " + broadcastJson + " ===");
            
            broadcastMessage(broadcastJson);
            
            System.out.println("=== 用户信息处理完成 ===");
            
        } catch (Exception e) {
            System.out.println("=== 处理用户信息时发生异常: " + e.getMessage() + " ===");
            e.printStackTrace();
            sendSimpleErrorMessage(session, "处理用户信息失败: " + e.getMessage());
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
            
            boolean hasImage = false;
            String imageData = null;
            String imageFieldName = null;
            
            if (originalPayload.containsKey("isImage") && Boolean.TRUE.equals(originalPayload.get("isImage")) || 
                originalPayload.containsKey("imageBase64") ||
                originalPayload.containsKey("image")) {
                hasImage = true;
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
            
            for (Map.Entry<String, Object> entry : originalPayload.entrySet()) {
                String key = entry.getKey();
                if ("userId".equals(key)) {
                    System.out.println("=== 排除字段: userId ===");
                    continue;
                }
                Object value = entry.getValue();
                if (value != null) {
                    String stringValue = value.toString();
                    processedPayload.put(key, stringValue);
                }
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
            if (originalPayload.containsKey("userId")) {
                String userIdStr = String.valueOf(originalPayload.get("userId"));
                try {
                    Long userId = Long.parseLong(userIdStr);
                    if (userService != null) {
                        User user = userService.findUserById(userId);
                        if (user != null && user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                            String avatar = user.getAvatar();
                            if (avatar.startsWith("/uploads/avatars/")) {
                                avatarUrl = avatar;
                            } else {
                                avatarUrl = "/uploads/avatars/" + avatar;
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    System.out.println("=== 用户ID格式错误 ===");
                }
            }
            
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
                    Long fromUserId = sessionUserMap.get(session);
                    if (fromUserId != null) {
                        chatMessageService.saveMessage(fromUserId, 0L, processedPayload.get("message"), "chat");
                    }
                } catch (Exception e) {
                    System.out.println("=== 保存群聊消息失败: " + e.getMessage() + " ===");
                }
            }
            
            System.out.println("=== 聊天消息广播完成 ===");
            
        } catch (Exception e) {
            System.out.println("=== 处理聊天消息时发生异常: " + e.getMessage() + " ===");
            e.printStackTrace();
            sendSimpleErrorMessage(session, "处理消息失败: " + e.getMessage());
        }
    }
    
    private void handlePrivateMessage(Map<String, Object> messageMap, Session session) {
        System.out.println("=== 开始处理私聊消息 ===");
        System.out.println("=== 当前sessionUserMap内容: " + sessionUserMap + " ===");
        System.out.println("=== 当前session: " + session + " ===");
        System.out.println("=== sessionUserMap.get(session): " + sessionUserMap.get(session) + " ===");
        try {
            if (!messageMap.containsKey("payload")) {
                sendSimpleErrorMessage(session, "私聊消息缺少payload字段");
                return;
            }
            
            Map<String, Object> originalPayload = (Map<String, Object>) messageMap.get("payload");
            System.out.println("=== 私聊消息payload: " + originalPayload + " ===");
            
            if (!originalPayload.containsKey("toUserId")) {
                sendSimpleErrorMessage(session, "私聊消息缺少目标用户ID");
                return;
            }
            
            Long fromUserId = sessionUserMap.get(session);
            System.out.println("=== 获取到的fromUserId: " + fromUserId + " ===");
            if (fromUserId == null) {
                System.out.println("=== fromUserId为null，sessionUserMap中没有该session的映射 ===");
                sendSimpleErrorMessage(session, "请先登录");
                return;
            }
            
            Long toUserId = Long.parseLong(originalPayload.get("toUserId").toString());
            
            if (fromUserId.equals(toUserId)) {
                sendSimpleErrorMessage(session, "不能给自己发送私聊消息");
                return;
            }
            
            if (friendshipService != null && !friendshipService.areFriends(fromUserId, toUserId)) {
                sendSimpleErrorMessage(session, "对方不是你的好友，无法发送私聊消息");
                return;
            }
            
            Session targetSession = userSessionMap.get(toUserId);
            if (targetSession == null || !targetSession.isOpen()) {
                sendSimpleErrorMessage(session, "对方不在线");
                return;
            }
            
            Map<String, String> processedPayload = new HashMap<>();
            
            for (Map.Entry<String, Object> entry : originalPayload.entrySet()) {
                String key = entry.getKey();
                if ("toUserId".equals(key)) {
                    continue;
                }
                Object value = entry.getValue();
                if (value != null) {
                    processedPayload.put(key, value.toString());
                }
            }
            
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            processedPayload.put("time", time);
            processedPayload.put("fromUserId", fromUserId.toString());
            
            User fromUser = userService.findUserById(fromUserId);
            if (fromUser != null) {
                processedPayload.put("username", fromUser.getUsername());
                String avatar = fromUser.getAvatar();
                if (avatar != null && !avatar.isEmpty()) {
                    processedPayload.put("avatar", avatar.startsWith("/uploads/") ? avatar : "/uploads/avatars/" + avatar);
                } else {
                    processedPayload.put("avatar", "/uploads/avatars/default.png");
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "private");
            response.put("payload", processedPayload);
            
            String privateJson = objectMapper.writeValueAsString(response);
            
            sendMessageToUser(targetSession, privateJson);
            sendMessageToUser(session, privateJson);
            
            if (chatMessageService != null && processedPayload.containsKey("message")) {
                try {
                    chatMessageService.saveMessage(fromUserId, toUserId, processedPayload.get("message"), "private");
                } catch (Exception e) {
                    System.out.println("=== 保存私聊消息失败: " + e.getMessage() + " ===");
                }
            }
            
            System.out.println("=== 私聊消息发送完成，从 " + fromUserId + " 到 " + toUserId + " ===");
            
        } catch (Exception e) {
            System.out.println("=== 处理私聊消息时发生异常: " + e.getMessage() + " ===");
            e.printStackTrace();
            sendSimpleErrorMessage(session, "发送私聊消息失败: " + e.getMessage());
        }
    }
    
    private void sendSimpleErrorMessage(Session session, String errorMessage) {
        try {
            if (session != null && session.isOpen()) {
                String errorJson = "{\"type\":\"error\",\"payload\":{\"message\":\"" + 
                    escapeJson(errorMessage) + "\"}}";
                session.getBasicRemote().sendText(errorJson);
                System.out.println("[错误消息] 已发送错误提示: " + errorMessage);
            }
        } catch (Exception e) {
            System.out.println("[错误消息] 发送错误消息失败: " + e.getMessage());
        }
    }
    
    private void sendMessageToUser(Session session, String message) {
        try {
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(message);
                System.out.println("[发送消息] 已发送消息给用户");
            } else {
                System.out.println("[发送消息] 会话已关闭，无法发送消息");
            }
        } catch (Exception e) {
            System.out.println("[发送消息] 发送消息失败: " + e.getMessage());
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
                    Long userId = sessionUserMap.remove(sessionToRemove);
                    if (userId != null) {
                        userSessionMap.remove(userId);
                    }
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
    
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (ch < 32) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }
        return sb.toString();
    }
    
    private String saveBase64ImageExtremelySafe(String base64Data) {
        try {
            String shortUuid = UUID.randomUUID().toString().substring(0, 8);
            return "image_" + shortUuid + ".jpg";
        } catch (Throwable t) {
            return "safe_image.jpg";
        }
    }
    
    public static void sendFriendRequestNotification(Long toUserId, Long fromUserId, String fromUsername, String fromAvatar, Long friendshipId) {
        Session targetSession = userSessionMap.get(toUserId);
        if (targetSession != null && targetSession.isOpen()) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("fromUserId", fromUserId);
                payload.put("fromUsername", fromUsername);
                payload.put("fromAvatar", fromAvatar);
                payload.put("friendshipId", friendshipId);
                payload.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                
                Map<String, Object> message = new HashMap<>();
                message.put("type", "friendRequest");
                message.put("payload", payload);
                
                String json = objectMapper.writeValueAsString(message);
                targetSession.getBasicRemote().sendText(json);
                System.out.println("=== 已发送好友请求通知给用户: " + toUserId + " ===");
            } catch (Exception e) {
                System.out.println("=== 发送好友请求通知失败: " + e.getMessage() + " ===");
            }
        } else {
            System.out.println("=== 目标用户不在线，无法发送好友请求通知: " + toUserId + " ===");
        }
    }
    
    public static void sendFriendAcceptedNotification(Long toUserId, Long fromUserId, String fromUsername, String fromAvatar) {
        Session targetSession = userSessionMap.get(toUserId);
        if (targetSession != null && targetSession.isOpen()) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("fromUserId", fromUserId);
                payload.put("fromUsername", fromUsername);
                payload.put("fromAvatar", fromAvatar);
                payload.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                
                Map<String, Object> message = new HashMap<>();
                message.put("type", "friendAccepted");
                message.put("payload", payload);
                
                String json = objectMapper.writeValueAsString(message);
                targetSession.getBasicRemote().sendText(json);
                System.out.println("=== 已发送好友接受通知给用户: " + toUserId + " ===");
            } catch (Exception e) {
                System.out.println("=== 发送好友接受通知失败: " + e.getMessage() + " ===");
            }
        }
    }
}
