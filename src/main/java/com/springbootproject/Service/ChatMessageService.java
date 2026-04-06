package com.springbootproject.Service;

import com.springbootproject.Entity.ChatMessage;
import com.springbootproject.Repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatMessageService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    public ChatMessage saveMessage(Long userId, String username, String message, String avatar) {
        ChatMessage chatMessage = new ChatMessage(userId, username, message, avatar);
        return chatMessageRepository.save(chatMessage);
    }

    public List<ChatMessage> getRecentMessages() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        return chatMessageRepository.findByCreateTimeAfterOrderByCreateTimeAsc(oneDayAgo);
    }

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void deleteOldMessages() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        int deleted = chatMessageRepository.deleteOldMessages(oneDayAgo);
        System.out.println("=== 已删除 " + deleted + " 条超过一天的聊天记录 ===");
    }
}
