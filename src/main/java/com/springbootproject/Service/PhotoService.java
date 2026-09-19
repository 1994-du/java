package com.springbootproject.Service;

import com.springbootproject.Entity.Photo;
import com.springbootproject.Repository.PhotoRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final PhotoStorageService storageService;

    public PhotoService(PhotoRepository photoRepository, PhotoStorageService storageService) {
        this.photoRepository = photoRepository;
        this.storageService = storageService;
    }

    /**
     * 保存上传的图片到对应用户目录，并在数据库记录一条。
     */
    public Photo save(Long userId, String username, MultipartFile file) throws IOException {
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        String stored = UUID.randomUUID().toString() + ext;

        Path dir = storageService.getUserDir(userId);
        Path target = dir.resolve(stored);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        Photo p = new Photo();
        p.setUserId(userId);
        p.setUsername(username);
        p.setOriginalFilename(original != null && !original.isBlank() ? original : stored);
        p.setStoredFilename(stored);
        p.setContentType(file.getContentType());
        p.setSize(file.getSize());
        p.setUploadTime(LocalDateTime.now());
        p.setDownloaded(false);
        return photoRepository.save(p);
    }

    public List<Photo> listByUser(Long userId) {
        return photoRepository.findByUserIdOrderByUploadTimeDesc(userId);
    }

    public Optional<Photo> findByIdAndUser(Long id, Long userId) {
        return photoRepository.findByUserIdAndId(userId, id);
    }

    public Path resolveFile(Photo p) throws IOException {
        return storageService.getUserDir(p.getUserId()).resolve(p.getStoredFilename());
    }

    /**
     * 删除指定文件记录及其磁盘文件（仅限该用户自己的文件）。
     */
    public void deleteByUserAndIds(Long userId, List<Long> ids) {
        for (Long id : ids) {
            deleteOne(userId, id);
        }
    }

    /**
     * 取件确认：PC 端把图片保存到本地成功后调用，服务端随即删除该中转文件与记录。
     * <p>
     * 与 {@link #deleteByUserAndIds} 的机制相同，区别在语义：本方法服务于
     * "下载 → 落盘成功 → 确认删除" 这条链路，只有在客户端确认保存成功后才删，
     * 因此下载中断不会误删服务器上的源文件。
     *
     * @return 实际删除的条数（id 不属于该用户或已不存在时不计入）
     */
    public int confirmAndDelete(Long userId, List<Long> ids) {
        int deleted = 0;
        for (Long id : ids) {
            if (id != null && deleteOne(userId, id)) {
                deleted++;
            }
        }
        return deleted;
    }

    /**
     * 删除单条记录及其磁盘文件。归属校验内联在查询里，拿不到别人的文件。
     *
     * @return true 表示确实删掉了一条
     */
    private boolean deleteOne(Long userId, Long id) {
        Optional<Photo> opt = photoRepository.findByUserIdAndId(userId, id);
        if (opt.isEmpty()) {
            return false;
        }
        Photo p = opt.get();
        try {
            Path f = storageService.getUserDir(userId).resolve(p.getStoredFilename());
            Files.deleteIfExists(f);
        } catch (IOException ignored) {
            // 磁盘文件删不掉也不影响数据库记录清理
        }
        photoRepository.delete(p);
        return true;
    }

    public void markDownloaded(Long userId, Long id) {
        photoRepository.findByUserIdAndId(userId, id).ifPresent(p -> {
            p.setDownloaded(true);
            photoRepository.save(p);
        });
    }
}
