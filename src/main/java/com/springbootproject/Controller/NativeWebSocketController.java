package com.springbootproject.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springbootproject.Config.WebSocketConfig;
import com.springbootproject.Entity.User;
import com.springbootproject.Service.UserService;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ServerEndpoint(value = "/ws", configurator = WebSocketConfig.class)
public class NativeWebSocketController {
    
    private static UserService userService;
    
    @Autowired
    public void setUserService(UserService userService) {
        NativeWebSocketController.userService = userService;
    }
    
    private static final CopyOnWriteArraySet<Session> webSocketSet = new CopyOnWriteArraySet<>();
    private static final AtomicInteger onlineCount = new AtomicInteger(0);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("=== WebSocket新连接加入 ===");
        if (session != null) {
            webSocketSet.add(session);
            onlineCount.incrementAndGet();
            System.out.println("=== WebSocket在线人数增加为: " + onlineCount.get() + " ===");
            // 发送欢迎消息
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
                // 移除会话
                webSocketSet.remove(session);
                // 减少在线人数
                onlineCount.decrementAndGet();
                System.out.println("在线人数减少为: " + onlineCount.get());
                // 关闭会话
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
        
        // 极度简化的消息处理，确保不会因为异常而关闭连接
        try {
            // 第一步：快速检查消息长度，避免处理过大的消息
            if (message.length() > 1024 * 1024 * 5) { // 5MB限制
                System.out.println("[消息拒绝] 消息过大，发送错误提示");
                sendSimpleErrorMessage(session, "消息过大，最大支持5MB");
                return;
            }
            
            // 第二步：安全地解析消息，使用try-catch隔离每个可能的异常点
            Map<String, Object> messageMap = null;
            try {
                messageMap = objectMapper.readValue(message, Map.class);
                System.out.println("[JSON解析] 成功解析JSON消息");
            } catch (Exception e) {
                System.out.println("[JSON解析失败] 消息不是有效的JSON格式: " + e.getMessage());
                sendSimpleErrorMessage(session, "消息格式错误，请发送有效的JSON格式");
                return;
            }
            
            // 第三步：检查必要字段 - 添加调试日志
            System.out.println("[消息验证] 接收到的messageMap: " + messageMap);
            if (messageMap == null || !messageMap.containsKey("type")) {
                System.out.println("[消息验证] 消息缺少type字段，messageMap内容: " + messageMap);
                sendSimpleErrorMessage(session, "消息缺少必要的type字段");
                return;
            }
            
            String messageType = messageMap.get("type").toString();
            System.out.println("[消息类型] 处理消息类型: " + messageType);
            
            // 第四步：根据消息类型进行处理
            switch (messageType) {
                case "userInfo":
                case "username": // 添加对username类型的支持，使用相同的处理方法
                    handleUserInfoMessage(messageMap, session);
                    break;
                case "chat":
                    handleChatMessage(messageMap, session);
                    break;
                default:
                    System.out.println("[消息类型] 未知消息类型: " + messageType);
                    sendSimpleErrorMessage(session, "不支持的消息类型");
            }
            
        } catch (Throwable t) { // 捕获包括Error在内的所有异常
            // 最终的异常捕获，确保不会关闭连接
            System.out.println("[致命异常] 处理消息时发生严重错误: " + t.getMessage());
            // 不再尝试发送消息，因为连接可能已经不稳定
        }
        
        System.out.println("[消息处理完成] 消息处理流程结束");
    }
    
    /**
     * 处理用户信息消息
     */
    private void handleUserInfoMessage(Map<String, Object> messageMap, Session session) {
        System.out.println("=== 开始处理用户信息消息 ===");
        try {
            if (!messageMap.containsKey("payload")) {
                System.out.println("=== 错误：用户信息消息缺少payload字段 ===");
                sendSimpleErrorMessage(session, "用户信息消息缺少payload字段");
                return;
            }
            
            Map<String, Object> payload = (Map<String, Object>) messageMap.get("payload");
            String username = payload.getOrDefault("username", "匿名用户").toString();
            System.out.println("=== 接收到用户名: " + username + " ===");
            
            // 安全地设置用户属性
            if (session != null) {
                session.getUserProperties().put("username", username);
                System.out.println("=== 成功设置用户属性: " + username + " ===");
            }
            
            // 构建用户加入消息并广播
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            
            // 使用StringBuilder构建JSON，避免字符串拼接错误
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
            
            // 直接广播消息
            broadcastMessage(broadcastJson);
            
            // 移除了用户信息确认消息的发送
            System.out.println("=== 用户信息处理完成，不发送确认消息 ===");
            
        } catch (Exception e) {
            System.out.println("=== 处理用户信息时发生异常: " + e.getMessage() + " ===");
            e.printStackTrace();
            // 发送错误消息给用户
            sendSimpleErrorMessage(session, "处理用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理聊天消息 - 保留原始payload中的所有字段
     */
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
            System.out.println("=== payload字段列表: " + String.join(", ", originalPayload.keySet()) + " ===");
            
            // 检查是否包含图片数据
            boolean hasImage = false;
            String imageData = null;
            String imageFieldName = null;
            
            // 首先检查isImage标志或任何可能的图片字段
            if (originalPayload.containsKey("isImage") && Boolean.TRUE.equals(originalPayload.get("isImage")) || 
                originalPayload.containsKey("imageBase64") ||
                originalPayload.containsKey("image")) {
                hasImage = true;
                System.out.println("=== 检测到图片消息 ===");
                
                // 尝试所有可能的图片字段名，按优先级顺序
                if (originalPayload.containsKey("image")) {
                    imageData = String.valueOf(originalPayload.get("image"));
                    imageFieldName = "image";
                } else if (originalPayload.containsKey("imageBase64")) {
                    imageData = String.valueOf(originalPayload.get("imageBase64"));
                    imageFieldName = "imageBase64";
                }
                
                // 记录使用的字段和数据长度
                if (imageData != null) {
                    System.out.println("=== 使用" + imageFieldName + "字段，数据长度: " + imageData.length() + " 字符 ===");
                    // 检查是否包含有效的data URL前缀
                    boolean hasValidPrefix = imageData.startsWith("data:image/");
                    System.out.println("=== 图片数据有效前缀: " + hasValidPrefix + " ===");
                }
            }
            
            // 准备构建新payload，但不包含userId字段
            Map<String, String> processedPayload = new HashMap<>();
            
            // 1. 复制原始payload中的所有字段，但排除userId
            for (Map.Entry<String, Object> entry : originalPayload.entrySet()) {
                String key = entry.getKey();
                // 跳过userId字段
                if ("userId".equals(key)) {
                    System.out.println("=== 排除字段: userId ===");
                    continue;
                }
                Object value = entry.getValue();
                if (value != null) {
                    String stringValue = value.toString();
                    processedPayload.put(key, stringValue);
                    System.out.println("=== 复制原始字段: " + key + " = " + stringValue + " ===");
                }
            }
            
            // 2. 然后设置系统必要字段，但如果原始payload中已经有这些字段，则保留原始值
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            if (!processedPayload.containsKey("time")) {
                processedPayload.put("time", time);
            }
            
            // 3. 如果原始payload中没有username，但session中有，则使用session中的username
            if (!processedPayload.containsKey("username")) {
                String username = "匿名用户";
                if (session != null && session.getUserProperties().containsKey("username")) {
                    username = session.getUserProperties().get("username").toString();
                }
                processedPayload.put("username", username);
                System.out.println("=== 使用系统用户名: " + username + " ===");
            } else {
                System.out.println("=== 使用原始payload中的用户名: " + processedPayload.get("username") + " ===");
            }
            
            // 4. 从原始payload中获取userId用于查询头像，但不包含在返回的消息中
            String avatarUrl = "/uploads/avatars/default.png"; // 默认头像
            if (originalPayload.containsKey("userId")) {
                String userIdStr = String.valueOf(originalPayload.get("userId"));
                try {
                    Long userId = Long.parseLong(userIdStr);
                    System.out.println("=== 从原始payload检测到用户ID: " + userId + "，尝试从数据库查询头像 ===");
                    
                    // 使用UserService的findUserById方法从数据库查询用户信息
                    if (userService != null) {
                        User user = userService.findUserById(userId);
                        if (user != null && user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                            // 检查头像路径是否已经包含/uploads/avatars/前缀，避免重复
                            String avatar = user.getAvatar();
                            if (avatar.startsWith("/uploads/avatars/")) {
                                avatarUrl = avatar;
                            } else {
                                avatarUrl = "/uploads/avatars/" + avatar;
                            }
                            System.out.println("=== 为用户ID: " + userId + " 设置头像: " + avatar + " ===");
                        } else {
                            System.out.println("=== 用户ID: " + userId + " 没有设置头像或未找到用户信息，使用默认头像 ===");
                        }
                    } else {
                        System.out.println("=== UserService未初始化，无法查询用户头像，使用默认头像 ===");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("=== 用户ID格式错误: " + userIdStr + "，使用默认头像 ===");
                } catch (Exception e) {
                    System.out.println("=== 获取用户头像异常: " + e.getMessage() + "，使用默认头像 ===");
                }
            } else {
                System.out.println("=== 消息中未包含userId，使用默认头像 ===");
            }
            
            // 设置头像到processedPayload中，无论是否有userId都包含头像字段
            processedPayload.put("avatar", avatarUrl);
            System.out.println("=== 设置头像URL: " + avatarUrl + " ===");
            
            // 处理图片数据 - 更加健壮的实现
            if (imageData != null) {
                try {
                    System.out.println("=== 收到图片消息，数据长度: " + imageData.length() + " ===");
                    
                    // 安全地处理图片，不进行实际的Base64解码
                    String imageUrl = saveBase64ImageExtremelySafe(imageData);
                    System.out.println("=== 图片处理完成，模拟的图片URL: " + imageUrl + " ===");
                    processedPayload.put("imageUrl", imageUrl);
                    
                    // 只保留image字段，不再使用imageBase64字段
                    processedPayload.put("image", imageData);
                    
                    System.out.println("=== 图片数据已添加到响应中 ===");
                    
                } catch (Throwable t) { // 捕获所有异常，确保不会因为图片处理失败而关闭连接
                    System.out.println("=== 处理图片数据时发生异常，但不会影响连接: " + t.getMessage() + " ===");
                    // 即使图片处理失败，也继续处理消息的其他部分
                }
            }
            
            // 已经在步骤1中复制了所有原始字段，这里不再重复处理
            
            // 4. 确保message字段存在（如果content字段有值）
            if (!processedPayload.containsKey("message") && originalPayload.containsKey("content")) {
                String content = originalPayload.get("content").toString();
                processedPayload.put("message", content);
                System.out.println("=== 从content字段设置message: " + content + " ===");
            }
            
            // 5. 使用ObjectMapper构建完整的JSON，确保格式正确
            Map<String, Object> response = new HashMap<>();
            response.put("type", "chat");
            response.put("payload", processedPayload);
            
            String chatJson = objectMapper.writeValueAsString(response);
            System.out.println("=== 聊天消息JSON: " + chatJson + " ===");
            
            // 广播聊天消息
            broadcastMessage(chatJson);
            System.out.println("=== 聊天消息广播完成 ===");
            
        } catch (Exception e) {
            System.out.println("=== 处理聊天消息时发生异常: " + e.getMessage() + " ===");
            e.printStackTrace();
            // 发送错误消息给用户
            sendSimpleErrorMessage(session, "处理消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送简单的错误消息给用户
     */
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
    
    /**
     * 发送消息给指定用户
     */
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
    
    /**
     * 广播消息给所有在线客户端 - 极度安全的版本
     */
    private void broadcastMessage(String message) {
        System.out.println("[广播开始] 准备广播消息，消息长度: " + message.length());
        
        // 基本的安全检查
        if (message == null || message.isEmpty()) {
            System.out.println("[广播] 消息为空，跳过");
            return;
        }
        
        // 限制消息大小，避免过大的消息导致问题
        if (message.length() > 1024 * 1024 * 2) { // 2MB限制
            System.out.println("[广播] 消息过长，截断到2MB");
            message = message.substring(0, 1024 * 1024 * 2) + "...[消息过长已截断]";
        }
        
        // 安全地获取连接列表
        if (webSocketSet == null || webSocketSet.isEmpty()) {
            System.out.println("[广播] 没有活跃连接");
            return;
        }
        
        // 创建一个副本，避免并发修改问题
        List<Session> sessionsToProcess = new ArrayList<>();
        try {
            sessionsToProcess.addAll(webSocketSet);
            System.out.println("[广播] 复制连接列表，共 " + sessionsToProcess.size() + " 个连接");
        } catch (Exception e) {
            System.out.println("[广播] 复制连接列表失败: " + e.getMessage());
            return;
        }
        
        // 记录需要移除的会话
        List<Session> sessionsToRemove = new ArrayList<>();
        
        // 遍历发送消息
        for (Session session : sessionsToProcess) {
            try {
                // 检查会话是否有效
                if (session == null || !session.isOpen()) {
                    System.out.println("[广播] 会话无效，标记为移除");
                    sessionsToRemove.add(session);
                    continue;
                }
                
                // 发送消息
                session.getBasicRemote().sendText(message);
                System.out.println("[广播] 成功发送消息到一个客户端");
                
            } catch (Exception e) {
                System.out.println("[广播] 发送消息到客户端失败: " + e.getMessage());
                // 记录失败的会话，稍后移除
                if (session != null) {
                    sessionsToRemove.add(session);
                }
            }
        }
        
        // 清理无效的会话
        if (!sessionsToRemove.isEmpty()) {
            System.out.println("[广播] 清理 " + sessionsToRemove.size() + " 个无效会话");
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
            // 更新在线人数
            onlineCount.set(Math.max(0, onlineCount.get() - sessionsToRemove.size()));
            System.out.println("[广播] 清理后在线人数: " + onlineCount.get());
        }
        
        System.out.println("[广播结束] 消息广播完成");
    }
    
    /**
     * 转义JSON字符串中的特殊字符
     */
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
                    // 处理控制字符
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
    
    /**
     * 极度安全的Base64图片处理方法
     * 这个方法确保不会抛出任何异常，并且立即返回一个模拟的文件名
     */
    private String saveBase64ImageExtremelySafe(String base64Data) {
        try {
            System.out.println("[图片处理-安全] 进入极度安全的图片处理方法");
            
            // 不做任何Base64解码或文件操作
            // 直接生成一个简单的UUID作为文件名
            String shortUuid = UUID.randomUUID().toString().substring(0, 8);
            String safeFileName = "image_" + shortUuid + ".jpg";
            
            System.out.println("[图片处理-安全] 生成安全的图片文件名: " + safeFileName);
            return safeFileName;
            
        } catch (Throwable t) { // 捕获包括Error在内的所有异常
            // 即使发生最严重的错误，也返回一个默认的文件名
            System.out.println("[图片处理-安全] 发生严重错误，但仍返回默认文件名");
            return "safe_image.jpg";
        }
    }
}