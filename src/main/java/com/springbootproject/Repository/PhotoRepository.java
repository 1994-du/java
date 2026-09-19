package com.springbootproject.Repository;

import com.springbootproject.Entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByUserIdOrderByUploadTimeDesc(Long userId);

    Optional<Photo> findByUserIdAndId(Long userId, Long id);

    /** 上传时间早于 cutoff 的中转文件，用于兜底清理 PC 端未确认删除的残留 */
    List<Photo> findByUploadTimeBefore(LocalDateTime cutoff);
}
