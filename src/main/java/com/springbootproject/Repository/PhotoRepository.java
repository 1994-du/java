package com.springbootproject.Repository;

import com.springbootproject.Entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByUserIdOrderByUploadTimeDesc(Long userId);

    Optional<Photo> findByUserIdAndId(Long userId, Long id);
}
