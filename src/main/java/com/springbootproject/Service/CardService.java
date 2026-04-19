package com.springbootproject.Service;

import com.springbootproject.Entity.Card;
import com.springbootproject.Repository.CardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class CardService {

    @Autowired
    private CardRepository cardRepository;

    // 保存卡片
    public Card saveCard(Card card) {
        card.setCreateTime(new Date());
        return cardRepository.save(card);
    }

    // 根据ID获取卡片模板
    public Card getCardById(Long cardId) {
        return cardRepository.findById(cardId).orElse(null);
    }

    // 获取用户收到的卡片列表（包括广播卡片）
    public List<Card> getReceivedCards(Long userId) {
        // 先获取用户自己的卡片
        List<Card> userCards = cardRepository.findByReceiverIdOrderByCreateTimeDesc(userId);
        // 再获取广播卡片（receiverId为null）
        List<Card> broadcastCards = cardRepository.findByReceiverIdOrderByCreateTimeDesc(null);
        // 合并两个列表
        userCards.addAll(broadcastCards);
        // 按创建时间倒序排序
        userCards.sort((c1, c2) -> c2.getCreateTime().compareTo(c1.getCreateTime()));
        return userCards;
    }

    // 获取用户收到的未读卡片（包括广播卡片）
    public List<Card> getUnreadCards(Long userId) {
        // 先获取用户自己的未读卡片
        List<Card> userCards = cardRepository.findByReceiverIdAndReadStatusFalseOrderByCreateTimeDesc(userId);
        // 再获取广播的未读卡片（receiverId为null）
        List<Card> broadcastCards = cardRepository.findByReceiverIdAndReadStatusFalseOrderByCreateTimeDesc(null);
        // 合并两个列表
        userCards.addAll(broadcastCards);
        // 按创建时间倒序排序
        userCards.sort((c1, c2) -> c2.getCreateTime().compareTo(c1.getCreateTime()));
        return userCards;
    }

    // 标记卡片为已读
    public void markAsRead(Long cardId) {
        Card card = cardRepository.findById(cardId).orElse(null);
        if (card != null) {
            card.setReadStatus(true);
            cardRepository.save(card);
        }
    }

    // 获取未读卡片数量（包括广播卡片）
    public long getUnreadCount(Long userId) {
        // 计算用户自己的未读卡片数量
        long userCount = cardRepository.countByReceiverIdAndReadStatusFalse(userId);
        // 计算广播的未读卡片数量
        long broadcastCount = cardRepository.countByReceiverIdAndReadStatusFalse(null);
        return userCount + broadcastCount;
    }
}
