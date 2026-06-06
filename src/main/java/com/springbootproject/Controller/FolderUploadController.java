package com.springbootproject.Controller;

import com.springbootproject.Model.ApiResponse;
import com.springbootproject.Service.UploadStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FolderUploadController {

    private final UploadStorageService uploadStorageService;

    public FolderUploadController(UploadStorageService uploadStorageService) {
        this.uploadStorageService = uploadStorageService;
    }

    @PostMapping(value = "/upload-folder", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFolder(MultipartHttpServletRequest request) {
        try {
            List<MultipartFile> files = collectMultipartFiles(request);
            if (files.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("请选择要上传的文件夹"));
            }

            Path uploadRoot = uploadStorageService.getFilesDir().toAbsolutePath().normalize();
            Files.createDirectories(uploadRoot);

            String[] submittedPaths = request.getParameterValues("paths");
            List<FolderUploadItem> uploadItems = buildUploadItems(files, submittedPaths, uploadRoot);
            if (uploadItems.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("没有可上传的文件"));
            }

            List<Map<String, Object>> uploadedFiles = new ArrayList<>();
            long totalSize = 0;
            for (FolderUploadItem item : uploadItems) {
                Files.createDirectories(item.targetPath().getParent());
                Files.copy(item.file().getInputStream(), item.targetPath(), StandardCopyOption.REPLACE_EXISTING);

                totalSize += item.file().getSize();
                uploadedFiles.add(buildFileResult(item));
            }

            Map<String, Object> data = new HashMap<>();
            data.put("count", uploadedFiles.size());
            data.put("totalSize", totalSize);
            data.put("files", uploadedFiles);
            return ResponseEntity.ok(ApiResponse.success("文件夹上传成功", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(ApiResponse.error("文件夹保存失败: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("文件夹上传失败: " + e.getMessage()));
        }
    }

    private List<MultipartFile> collectMultipartFiles(MultipartHttpServletRequest request) {
        List<MultipartFile> files = new ArrayList<>();
        request.getMultiFileMap().values().forEach(files::addAll);
        return files;
    }

    private List<FolderUploadItem> buildUploadItems(List<MultipartFile> files, String[] submittedPaths, Path uploadRoot) {
        List<FolderUploadItem> uploadItems = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file.isEmpty()) {
                continue;
            }

            String submittedPath = getSubmittedPath(file, submittedPaths, i);
            Path relativePath = sanitizeRelativePath(submittedPath);
            Path targetPath = uploadRoot.resolve(relativePath).normalize();
            if (!targetPath.startsWith(uploadRoot)) {
                throw new IllegalArgumentException("非法文件路径: " + submittedPath);
            }

            uploadItems.add(new FolderUploadItem(file, relativePath, targetPath));
        }

        return uploadItems;
    }

    private String getSubmittedPath(MultipartFile file, String[] submittedPaths, int index) {
        if (submittedPaths != null && submittedPaths.length > index && StringUtils.hasText(submittedPaths[index])) {
            return submittedPaths[index];
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new IllegalArgumentException("无法获取文件名");
        }
        return originalFilename;
    }

    private Path sanitizeRelativePath(String submittedPath) {
        String cleanPath = submittedPath.replace('\\', '/').trim();
        while (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        if (!StringUtils.hasText(cleanPath)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }

        Path relativePath = Paths.get(cleanPath).normalize();
        if (relativePath.isAbsolute()
                || relativePath.getNameCount() == 0
                || ".".equals(relativePath.toString())
                || relativePath.startsWith("..")) {
            throw new IllegalArgumentException("非法文件路径: " + submittedPath);
        }

        return relativePath;
    }

    private Map<String, Object> buildFileResult(FolderUploadItem item) {
        Map<String, Object> fileResult = new HashMap<>();
        String relativePath = toUnixPath(item.relativePath());

        fileResult.put("originalFilename", item.file().getOriginalFilename());
        fileResult.put("relativePath", relativePath);
        fileResult.put("fileUrl", "/uploads/files/" + encodeUrlPath(relativePath));
        fileResult.put("size", item.file().getSize());
        return fileResult;
    }

    private String toUnixPath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private String encodeUrlPath(String relativePath) {
        String[] parts = relativePath.split("/");
        List<String> encodedParts = new ArrayList<>();
        for (String part : parts) {
            encodedParts.add(URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return String.join("/", encodedParts);
    }

    private record FolderUploadItem(MultipartFile file, Path relativePath, Path targetPath) {
    }
}
