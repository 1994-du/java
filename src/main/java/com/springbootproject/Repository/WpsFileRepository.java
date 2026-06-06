package com.springbootproject.Repository;

import com.springbootproject.Entity.WpsFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WpsFileRepository extends JpaRepository<WpsFile, Long> {
    
    Optional<WpsFile> findByWpsFileId(String wpsFileId);
    
    Optional<WpsFile> findByOriginalFilename(String originalFilename);
}