package com.springbootproject.Controller;

import com.springbootproject.Entity.Card;
import com.springbootproject.Entity.User;
import com.springbootproject.Model.ApiResponse;
import com.springbootproject.Service.CardService;
import com.springbootproject.Service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/card")
public class CardController {

    @Autowired
    private NativeWebSocketController webSocketController;

    @Autowired
    private UserService userService;

    @Autowired
    private CardService cardService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Object>> sendCard(@RequestBody Map<String, Object> request) {
        try {
            String title = (String) request.get("title");
            String content = (String) request.get("content");
            Long receiverId = null;
            if (request.containsKey("receiverId")) {
                try {
                    receiverId = Long.parseLong(request.get("receiverId").toString());
                } catch (NumberFormatException e) {
                    System.out.println("=== receiverId格式错误 ===");
                }
            }

            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("卡片标题不能为空"));
            }

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("卡片内容不能为空"));
            }

            Map<String, String> cardPayload = new HashMap<>();
            cardPayload.put("title", title);
            cardPayload.put("content", content);

            if (request.containsKey("buttonText")) {
                cardPayload.put("buttonText", request.get("buttonText").toString());
            }
            if (request.containsKey("buttonUrl")) {
                cardPayload.put("buttonUrl", request.get("buttonUrl").toString());
            }

            Long cardUserId = null;
            if (request.containsKey("userId")) {
                try {
                    cardUserId = Long.parseLong(request.get("userId").toString());
                    cardPayload.put("userId", cardUserId.toString());
                } catch (NumberFormatException e) {
                    System.out.println("=== userId格式错误 ===");
                }
            }

            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            cardPayload.put("time", time);

            String username = "匿名用户";
            String avatarUrl = "/uploads/avatars/default.png";
            
            if (cardUserId != null) {
                User user = userService.findUserById(cardUserId);
                if (user != null) {
                    username = user.getUsername() != null ? user.getUsername() : username;
                    if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                        String avatar = user.getAvatar();
                        if (avatar.startsWith("/uploads/")) {
                            avatarUrl = avatar;
                        } else {
                            avatarUrl = "/uploads/avatars/" + avatar;
                        }
                    }
                }
            }
            
            cardPayload.put("username", username);
            cardPayload.put("avatar", avatarUrl);
            
            if (receiverId != null) {
                cardPayload.put("receiverId", receiverId.toString());
            }

            // 保存卡片到数据库
            Card card = new Card();
            card.setTitle(title);
            card.setContent(content);
            card.setButtonText(request.containsKey("buttonText") ? request.get("buttonText").toString() : null);
            card.setButtonUrl(request.containsKey("buttonUrl") ? request.get("buttonUrl").toString() : null);
            card.setSenderId(cardUserId);
            card.setSenderName(username);
            card.setSenderAvatar(avatarUrl);
            card.setReceiverId(receiverId);
            cardService.saveCard(card);

            Map<String, Object> response = new HashMap<>();
            response.put("type", "card");
            response.put("payload", cardPayload);

            String cardJson = objectMapper.writeValueAsString(response);
            
            if (webSocketController != null) {
                webSocketController.broadcastCardMessage(cardJson);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("card", cardPayload);
            if (receiverId != null) {
                result.put("message", "卡片发送成功");
            } else {
                result.put("message", "卡片广播成功");
            }

            return ResponseEntity.ok(ApiResponse.success(result.get("message").toString(), result));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(ApiResponse.error("发送卡片失败: " + e.getMessage()));
        }
    }

    @PostMapping("/list")
    public ResponseEntity<ApiResponse<Object>> getCardList(@RequestBody Map<String, Object> request) {
        try {
            Long userId = null;
            if (request.containsKey("userId")) {
                try {
                    userId = Long.parseLong(request.get("userId").toString());
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("用户ID格式错误"));
                }
            }

            if (userId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户ID不能为空"));
            }

            List<Card> cards = cardService.getReceivedCards(userId);
            List<Map<String, Object>> cardList = new ArrayList<>();

            for (Card card : cards) {
                Map<String, Object> cardMap = new HashMap<>();
                cardMap.put("id", card.getId());
                cardMap.put("title", card.getTitle());
                cardMap.put("content", card.getContent());
                cardMap.put("buttonText", card.getButtonText());
                cardMap.put("buttonUrl", card.getButtonUrl());
                cardMap.put("senderId", card.getSenderId());
                cardMap.put("senderName", card.getSenderName());
                cardMap.put("senderAvatar", card.getSenderAvatar());
                cardMap.put("createTime", card.getCreateTime());
                cardMap.put("readStatus", card.isReadStatus());
                cardList.add(cardMap);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("cards", cardList);
            result.put("total", cardList.size());

            return ResponseEntity.ok(ApiResponse.success("获取卡片列表成功", result));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取卡片列表失败: " + e.getMessage()));
        }
    }

    @PostMapping("/unread")
    public ResponseEntity<ApiResponse<Object>> getUnreadCards(@RequestBody Map<String, Object> request) {
        try {
            Long userId = null;
            if (request.containsKey("userId")) {
                try {
                    userId = Long.parseLong(request.get("userId").toString());
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("用户ID格式错误"));
                }
            }

            if (userId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户ID不能为空"));
            }

            List<Card> cards = cardService.getUnreadCards(userId);
            long unreadCount = cardService.getUnreadCount(userId);

            List<Map<String, Object>> cardList = new ArrayList<>();
            for (Card card : cards) {
                Map<String, Object> cardMap = new HashMap<>();
                cardMap.put("id", card.getId());
                cardMap.put("title", card.getTitle());
                cardMap.put("content", card.getContent());
                cardMap.put("senderId", card.getSenderId());
                cardMap.put("senderName", card.getSenderName());
                cardMap.put("senderAvatar", card.getSenderAvatar());
                cardMap.put("createTime", card.getCreateTime());
                cardList.add(cardMap);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("cards", cardList);
            result.put("unreadCount", unreadCount);

            return ResponseEntity.ok(ApiResponse.success("获取未读卡片成功", result));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(ApiResponse.error("获取未读卡片失败: " + e.getMessage()));
        }
    }

    @PostMapping("/read")
    public ResponseEntity<ApiResponse<Object>> markAsRead(@RequestBody Map<String, Object> request) {
        try {
            Long cardId = null;
            if (request.containsKey("cardId")) {
                try {
                    cardId = Long.parseLong(request.get("cardId").toString());
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("卡片ID格式错误"));
                }
            }

            if (cardId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("卡片ID不能为空"));
            }

            cardService.markAsRead(cardId);

            return ResponseEntity.ok(ApiResponse.success("标记卡片为已读成功", null));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(ApiResponse.error("标记卡片为已读失败: " + e.getMessage()));
        }
    }
}
