package com.springbootproject.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 照片实体存储。与 UploadStorageService 区分：
 * - 每个用户独立目录：<base>/photos/<userId>/
 * - 该目录不在 WebMvcConfig 的静态资源映射内，只能通过鉴权后的接口访问，
 *   从而保证不同用户的文件互相隔离。
 */
@Service
public class PhotoStorageService {

    @Value("${app.photo-vault-dir:}")
    private String configuredDir;

    private Path baseDir;

    @PostConstruct
    public void init() throws IOException {
        if (configuredDir != null && !configuredDir.isBlank()) {
            baseDir = Paths.get(configuredDir).toAbsolutePath().normalize();
        } else {
            baseDir = Paths.get(System.getProperty("user.dir"), "photo-vault").toAbsolutePath().normalize();
        }
        Files.createDirectories(baseDir);
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public Path getUserDir(Long userId) throws IOException {
        Path dir = baseDir.resolve(String.valueOf(userId));
        Files.createDirectories(dir);
        return dir;
    }
}
