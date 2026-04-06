package com.springbootproject.Repository;

import com.springbootproject.Entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    List<ChatMessage> findByCreateTimeAfterOrderByCreateTimeAsc(LocalDateTime createTime);
    
    @Modifying
    @Query("DELETE FROM ChatMessage c WHERE c.createTime < :threshold")
    int deleteOldMessages(@Param("threshold") LocalDateTime threshold);
}
