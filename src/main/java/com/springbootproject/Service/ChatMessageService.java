package com.springbootproject.Service;

import com.springbootproject.Entity.ChatMessage;
import com.springbootproject.Entity.User;
import com.springbootproject.Repository.ChatMessageRepository;
import com.springbootproject.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatMessageService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    public ChatMessage saveMessage(Long fromUserId, Long toUserId, String message, String type) {
        ChatMessage chatMessage = new ChatMessage(fromUserId, toUserId, message, type);
        return chatMessageRepository.save(chatMessage);
    }

    public List<Map<String, Object>> getChatHistory(Long userId1, Long userId2) {
        List<ChatMessage> messages = chatMessageRepository.findChatHistory(userId1, userId2);
        return convertToResponse(messages, userId1);
    }

    public List<Map<String, Object>> getGroupChatHistory() {
        List<ChatMessage> messages = chatMessageRepository.findGroupChatHistory();
        return convertToResponse(messages, null);
    }

    public Map<String, Object> getRecentChatHistory(Long userId1, Long userId2, int page, int size) {
        System.out.println("=== ChatMessageService.getRecentChatHistory ===");
        System.out.println("=== 查询参数: userId1=" + userId1 + ", userId2=" + userId2 + ", page=" + page + ", size=" + size + " ===");
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> messagePage = chatMessageRepository.findRecentChatHistory(userId1, userId2, pageable);
        
        System.out.println("=== 查询结果: totalElements=" + messagePage.getTotalElements() + ", content.size=" + messagePage.getContent().size() + " ===");
        
        List<ChatMessage> messages = messagePage.getContent();
        List<Map<String, Object>> messageList = new ArrayList<>();
        
        for (ChatMessage msg : messages) {
            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("id", msg.getId());
            msgMap.put("fromUserId", msg.getFromUserId());
            msgMap.put("toUserId", msg.getToUserId());
            msgMap.put("message", msg.getMessage());
            msgMap.put("type", msg.getType());
            msgMap.put("createTime", msg.getCreateTime());
            
            User fromUser = userRepository.findById(msg.getFromUserId()).orElse(null);
            if (fromUser != null) {
                msgMap.put("fromUsername", fromUser.getUsername());
                String avatar = fromUser.getAvatar();
                msgMap.put("fromAvatar", avatar != null && !avatar.isEmpty() ? avatar : "/uploads/avatars/default.png");
            }
            
            messageList.add(msgMap);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", messageList);
        result.put("totalElements", messagePage.getTotalElements());
        result.put("totalPages", messagePage.getTotalPages());
        result.put("currentPage", page);
        
        return result;
    }

    public List<Map<String, Object>> getRecentChatFriends(Long currentUserId) {
        List<Object[]> results = chatMessageRepository.findRecentChatFriendsJPQL(currentUserId);
        List<Map<String, Object>> friends = new ArrayList<>();
        
        for (Object[] row : results) {
            Long friendId = ((Number) row[0]).longValue();
            
            User friend = userRepository.findById(friendId).orElse(null);
            if (friend != null) {
                Map<String, Object> friendInfo = new LinkedHashMap<>();
                friendInfo.put("id", friend.getId());
                friendInfo.put("username", friend.getUsername());
                friendInfo.put("avatar", friend.getAvatar() != null && !friend.getAvatar().isEmpty() 
                    ? friend.getAvatar() : "/uploads/avatars/default.png");
                friendInfo.put("gender", friend.getGender());
                
                List<ChatMessage> messages = chatMessageRepository.findChatHistory(currentUserId, friendId);
                if (!messages.isEmpty()) {
                    ChatMessage lastMessage = messages.get(messages.size() - 1);
                    friendInfo.put("lastMessage", lastMessage.getMessage());
                    friendInfo.put("lastMessageTime", lastMessage.getCreateTime());
                }
                
                friends.add(friendInfo);
            }
        }
        
        return friends;
    }

    private List<Map<String, Object>> convertToResponse(List<ChatMessage> messages, Long currentUserId) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (ChatMessage msg : messages) {
            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("id", msg.getId());
            msgMap.put("fromUserId", msg.getFromUserId());
            msgMap.put("toUserId", msg.getToUserId());
            msgMap.put("message", msg.getMessage());
            msgMap.put("type", msg.getType());
            msgMap.put("createTime", msg.getCreateTime());
            
            User fromUser = userRepository.findById(msg.getFromUserId()).orElse(null);
            if (fromUser != null) {
                msgMap.put("fromUsername", fromUser.getUsername());
                String avatar = fromUser.getAvatar();
                msgMap.put("fromAvatar", avatar != null && !avatar.isEmpty() ? avatar : "/uploads/avatars/default.png");
            }
            
            result.add(msgMap);
        }
        
        return result;
    }
}
