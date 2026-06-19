package com.springbootproject.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sys_chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    
    private String username;
    
    @Column(length = 5000)
    private String message;
    
    private String avatar;

    @Column(length = 1000)
    private String imageUrl;

    @Column(length = 1000)
    private String voiceUrl;

    @Column(length = 1000)
    private String audioUrl;

    @Column(length = 1000)
    private String recordUrl;

    @Column(length = 1000)
    private String mediaUrl;

    @Column(length = 50)
    private String messageType;

    private Integer duration;
    
    private LocalDateTime createTime;

    public ChatMessage() {
    }

    public ChatMessage(Long userId, String username, String message, String avatar, String imageUrl,
                       String voiceUrl, String audioUrl, String recordUrl, String mediaUrl,
                       String messageType, Integer duration) {
        this.userId = userId;
        this.username = username;
        this.message = message;
        this.avatar = avatar;
        this.imageUrl = imageUrl;
        this.voiceUrl = voiceUrl;
        this.audioUrl = audioUrl;
        this.recordUrl = recordUrl;
        this.mediaUrl = mediaUrl;
        this.messageType = messageType;
        this.duration = duration;
        this.createTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVoiceUrl() {
        return voiceUrl;
    }

    public void setVoiceUrl(String voiceUrl) {
        this.voiceUrl = voiceUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getRecordUrl() {
        return recordUrl;
    }

    public void setRecordUrl(String recordUrl) {
        this.recordUrl = recordUrl;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
