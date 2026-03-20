package com.springbootproject.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class UploadStorageService {

    private static final Path LEGACY_SHARED_UPLOAD_ROOT = Paths.get("/uploads");

    @Value("${app.upload.base-dir:}")
    private String configuredBaseDir;

    private Path baseUploadDir;

    @PostConstruct
    public void initialize() {
        try {
            baseUploadDir = resolveBaseUploadDir();
            Files.createDirectories(getAvatarDir());
            Files.createDirectories(getFilesDir());
            migrateLegacyDirectory(LEGACY_SHARED_UPLOAD_ROOT);
            migrateLegacyDirectory(Paths.get(System.getProperty("user.dir"), "uploads"));
            System.out.println("上传目录已初始化: " + baseUploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("初始化上传目录失败", e);
        }
    }

    public Path getAvatarDir() {
        return baseUploadDir.resolve("avatars");
    }

    public Path getFilesDir() {
        return baseUploadDir.resolve("files");
    }

    public String buildAvatarUrl(String filename) {
        return "/uploads/avatars/" + filename;
    }

    public String buildFileUrl(String filename) {
        return "/uploads/files/" + filename;
    }

    public String[] getResourceLocations() {
        Set<String> resourceLocations = new LinkedHashSet<>();
        addResourceLocation(resourceLocations, baseUploadDir);
        return resourceLocations.toArray(new String[0]);
    }

    private Path resolveBaseUploadDir() {
        if (configuredBaseDir != null && !configuredBaseDir.isBlank()) {
            return Paths.get(configuredBaseDir).toAbsolutePath().normalize();
        }

        return Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
    }

    private void migrateLegacyDirectory(Path legacyUploadDir) throws IOException {
        Path normalizedLegacyDir = legacyUploadDir.toAbsolutePath().normalize();
        if (normalizedLegacyDir.equals(baseUploadDir) || !Files.isDirectory(normalizedLegacyDir)) {
            return;
        }

        try (Stream<Path> pathStream = Files.walk(normalizedLegacyDir)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .forEach(sourceFile -> copyMissingFile(normalizedLegacyDir, sourceFile));
        }
    }

    private void copyMissingFile(Path legacyBaseDir, Path sourceFile) {
        try {
            Path relativePath = legacyBaseDir.relativize(sourceFile);
            Path targetFile = baseUploadDir.resolve(relativePath);
            if (Files.exists(targetFile)) {
                return;
            }
            Files.createDirectories(targetFile.getParent());
            Files.copy(sourceFile, targetFile, StandardCopyOption.COPY_ATTRIBUTES);
            System.out.println("已迁移历史上传文件: " + sourceFile + " -> " + targetFile);
        } catch (IOException e) {
            throw new IllegalStateException("迁移历史上传文件失败: " + sourceFile, e);
        }
    }

    private void addResourceLocation(Set<String> resourceLocations, Path path) {
        if (path != null && Files.isDirectory(path)) {
            resourceLocations.add(path.toAbsolutePath().normalize().toUri().toString());
        }
    }
}
