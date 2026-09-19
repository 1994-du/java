package com.springbootproject.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 照片中转记录：每位用户上传的图片都在数据库落一条记录，
 * 文件实体按 userId 分目录存放（见 PhotoStorageService），不通过静态资源公开。
 * 下载到本地后由客户端调用删除接口移除，服务器不永久保存。
 */
@Entity
@Table(name = "photos")
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "userId", nullable = false)
    private Long userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "originalFilename", nullable = false)
    private String originalFilename;

    @Column(name = "storedFilename", nullable = false)
    private String storedFilename;

    @Column(name = "contentType")
    private String contentType;

    @Column(name = "size")
    private Long size;

    @Column(name = "uploadTime", nullable = false, columnDefinition = "datetime(6)")
    private LocalDateTime uploadTime;

    @Column(name = "downloaded", nullable = false)
    private boolean downloaded = false;

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

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }
}
