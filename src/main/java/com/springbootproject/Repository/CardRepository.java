package com.springbootproject.Repository;

import com.springbootproject.Entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    
    // 查询用户收到的卡片列表
    List<Card> findByReceiverIdOrderByCreateTimeDesc(Long receiverId);
    
    // 查询用户收到的未读卡片
    List<Card> findByReceiverIdAndReadStatusFalseOrderByCreateTimeDesc(Long receiverId);
    
    // 标记卡片为已读
    long countByReceiverIdAndReadStatusFalse(Long receiverId);
}
