package com.springbootproject.Service;

import com.springbootproject.Entity.Photo;
import com.springbootproject.Repository.PhotoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 中转文件兜底清理。
 * <p>
 * 正常链路是 PC 端下载后调用 /api/photos/confirm/{id} 主动删除，服务器不留存。
 * 但如果 PC 端在下载后崩溃、断网或用户直接关掉程序，确认请求就发不出去，
 * 文件会永久残留在服务器上。此定时任务把超过保留期的中转文件清掉，避免磁盘无限增长。
 * <p>
 * 保留期由 app.photo-vault-retention-days 控制，设为 &lt;=0 可关闭清理。
 */
@Service
public class PhotoCleanupService {

    private static final Logger log = LoggerFactory.getLogger(PhotoCleanupService.class);

    private final PhotoRepository photoRepository;
    private final PhotoStorageService storageService;

    /** 中转文件保留天数；<=0 表示不清理 */
    @Value("${app.photo-vault-retention-days:7}")
    private int retentionDays;

    public PhotoCleanupService(PhotoRepository photoRepository, PhotoStorageService storageService) {
        this.photoRepository = photoRepository;
        this.storageService = storageService;
    }

    /** 每天凌晨 3:00 执行一次 */
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpired() {
        if (retentionDays <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<Photo> expired = photoRepository.findByUploadTimeBefore(cutoff);
        if (expired.isEmpty()) {
            return;
        }
        int removed = 0;
        for (Photo p : expired) {
            try {
                Path f = storageService.getUserDir(p.getUserId()).resolve(p.getStoredFilename());
                Files.deleteIfExists(f);
            } catch (IOException e) {
                log.warn("清理中转文件失败: id={} stored={}", p.getId(), p.getStoredFilename(), e);
            }
            photoRepository.delete(p);
            removed++;
        }
        log.info("中转文件清理完成：删除 {} 条（保留期 {} 天）", removed, retentionDays);
    }
}
