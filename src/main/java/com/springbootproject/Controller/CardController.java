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
            Long cardId = null;
            if (request.containsKey("cardId")) {
                try {
                    cardId = Long.parseLong(request.get("cardId").toString());
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("卡片ID格式错误"));
                }
            }
            
            Long receiverId = null;
            if (request.containsKey("receiverId")) {
                try {
                    receiverId = Long.parseLong(request.get("receiverId").toString());
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("接收者ID格式错误"));
                }
            }
            
            Long senderId = null;
            if (request.containsKey("senderId")) {
                try {
                    senderId = Long.parseLong(request.get("senderId").toString());
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("发送者ID格式错误"));
                }
            }
            
            if (cardId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("卡片ID不能为空"));
            }
            
            if (receiverId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("接收者ID不能为空"));
            }
            
            if (senderId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("发送者ID不能为空"));
            }
            
            Card cardTemplate = cardService.getCardById(cardId);
            if (cardTemplate == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("卡片不存在"));
            }
            
            String username = "匿名用户";
            String avatarUrl = "/uploads/avatars/default.png";
            User sender = userService.findUserById(senderId);
            if (sender != null) {
                username = sender.getUsername() != null ? sender.getUsername() : username;
                if (sender.getAvatar() != null && !sender.getAvatar().isEmpty()) {
                    String avatar = sender.getAvatar();
                    if (avatar.startsWith("/uploads/")) {
                        avatarUrl = avatar;
                    } else {
                        avatarUrl = "/uploads/avatars/" + avatar;
                    }
                }
            }
            
            Map<String, String> cardPayload = new HashMap<>();
            cardPayload.put("cardId", cardId.toString());
            cardPayload.put("senderId", senderId.toString());
            cardPayload.put("receiverId", receiverId.toString());
            cardPayload.put("title", cardTemplate.getTitle());
            cardPayload.put("content", cardTemplate.getContent());
            cardPayload.put("buttonText", cardTemplate.getButtonText());
            cardPayload.put("buttonUrl", cardTemplate.getButtonUrl());
            
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            cardPayload.put("time", time);
            cardPayload.put("username", username);
            cardPayload.put("avatar", avatarUrl);
            
            Card card = new Card();
            card.setTitle(cardTemplate.getTitle());
            card.setContent(cardTemplate.getContent());
            card.setButtonText(cardTemplate.getButtonText());
            card.setButtonUrl(cardTemplate.getButtonUrl());
            card.setSenderId(senderId);
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
            result.put("cardId", card.getId());
            result.put("message", "卡片发送成功");

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
