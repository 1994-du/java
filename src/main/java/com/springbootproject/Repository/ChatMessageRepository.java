package com.springbootproject.Repository;

import com.springbootproject.Entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.fromUserId = :userId1 AND m.toUserId = :userId2) OR " +
           "(m.fromUserId = :userId2 AND m.toUserId = :userId1) " +
           "ORDER BY m.createTime ASC")
    List<ChatMessage> findChatHistory(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    @Query("SELECT m FROM ChatMessage m WHERE m.type = 'chat' ORDER BY m.createTime ASC")
    List<ChatMessage> findGroupChatHistory();
    
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.fromUserId = :userId1 AND m.toUserId = :userId2) OR " +
           "(m.fromUserId = :userId2 AND m.toUserId = :userId1) " +
           "ORDER BY m.createTime DESC")
    Page<ChatMessage> findRecentChatHistory(@Param("userId1") Long userId1, @Param("userId2") Long userId2, Pageable pageable);
    
    @Query(value = "SELECT " +
           "CASE WHEN m.from_user_id = :userId THEN m.to_user_id ELSE m.from_user_id END as friend_id, " +
           "MAX(m.create_time) as last_message_time " +
           "FROM sys_chat_messages m " +
           "WHERE (m.from_user_id = :userId OR m.to_user_id = :userId) AND m.type = 'private' " +
           "GROUP BY friend_id " +
           "ORDER BY last_message_time DESC", nativeQuery = true)
    List<Object[]> findRecentChatFriends(@Param("userId") Long userId);
    
    @Query(value = "SELECT " +
           "CASE WHEN m.fromUserId = :userId THEN m.toUserId ELSE m.fromUserId END as friendId, " +
           "MAX(m.createTime) as lastMessageTime " +
           "FROM ChatMessage m " +
           "WHERE (m.fromUserId = :userId OR m.toUserId = :userId) AND m.type = 'private' " +
           "GROUP BY friendId " +
           "ORDER BY lastMessageTime DESC")
    List<Object[]> findRecentChatFriendsJPQL(@Param("userId") Long userId);
    
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.fromUserId = :userId OR m.toUserId = :userId) AND m.type = 'private' " +
           "ORDER BY m.createTime DESC")
    List<ChatMessage> findRecentMessagesByUserId(@Param("userId") Long userId);
}
