package com.springbootproject.Controller;

import com.springbootproject.Model.ApiResponse;
import com.springbootproject.Service.UploadStorageService;
import com.springbootproject.Service.WpsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class FileUploadController {

    // 最大文件大小 (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    // 允许的文件类型
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".pdf", ".doc", ".docx", ".txt", ".xlsx", ".xls", ".pptx", ".ppt"};
    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"};
    private static final String[] CHAT_UPLOAD_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".mp3", ".wav", ".m4a", ".aac", ".ogg", ".webm"};
    // WPS支持的文档类型
    private static final String[] WPS_EXTENSIONS = {".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".pdf", ".txt"};

    @Autowired
    private UploadStorageService uploadStorageService;

    @Autowired
    private WpsService wpsService;

    @PostMapping(value = "/api/file/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            validateFile(file);

            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename).toLowerCase();
            if (!isAllowedExtension(fileExtension)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("不支持的文件类型"));
            }

            Map<String, Object> data = saveFile(file, fileExtension, "file_");
            return ResponseEntity.ok(ApiResponse.success("文件上传成功", data));
        } catch (IOException e) {
            System.out.println("=== 文件上传失败 ===");
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("文件保存失败: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.out.println("=== 文件上传发生异常 ===");
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("文件上传失败: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/api/files/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            validateFile(file);

            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename).toLowerCase();
            if (!isImageExtension(fileExtension)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("仅支持图片文件"));
            }

            String contentType = file.getContentType();
            if (contentType != null && !contentType.isBlank() && !contentType.toLowerCase().startsWith("image/")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("仅支持图片文件"));
            }

            Map<String, Object> data = saveFile(file, fileExtension, "image_");
            return ResponseEntity.ok(ApiResponse.success("图片上传成功", data));
        } catch (IOException e) {
            System.out.println("=== 图片上传失败 ===");
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("图片保存失败: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.out.println("=== 图片上传发生异常 ===");
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("图片上传失败: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/api/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadChatImage(@RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "text", required = false) String text) {
        try {
            validateFile(file);

            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename).toLowerCase();
            String contentType = file.getContentType();
            if (!isChatUploadExtension(fileExtension)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("仅支持图片或语音文件"));
            }

            Map<String, Object> data = saveChatUpload(file, fileExtension, text, contentType);
            return ResponseEntity.ok(ApiResponse.success("success", data));
        } catch (IOException e) {
            System.out.println("=== 聊天文件上传失败 ===");
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("文件保存失败: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.out.println("=== 聊天文件上传发生异常 ===");
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("文件上传失败: " + e.getMessage()));
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过5MB");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new IllegalArgumentException("无法获取文件名");
        }
    }

    private Map<String, Object> saveFile(MultipartFile file, String fileExtension, String filenamePrefix) throws IOException {
        Path uploadDir = uploadStorageService.getFilesDir();
        Files.createDirectories(uploadDir);
        System.out.println("上传目录存在: " + uploadDir + ", 可写: " + Files.isWritable(uploadDir));

        String uniqueFilename = generateUniqueFilename(filenamePrefix, fileExtension);
        Path filePath = uploadDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = uploadStorageService.buildFileUrl(uniqueFilename);
        String originalFilename = file.getOriginalFilename();

        Map<String, Object> data = new HashMap<>();
        data.put("fileUrl", fileUrl);
        data.put("filename", uniqueFilename);
        data.put("originalFilename", originalFilename);
        data.put("size", file.getSize());
        data.put("contentType", file.getContentType());

        System.out.println("=== 文件上传成功 ===");
        System.out.println("原始文件名: " + originalFilename);
        System.out.println("保存文件名: " + uniqueFilename);
        System.out.println("文件大小: " + file.getSize() + " bytes");
        System.out.println("文件URL: " + fileUrl);

        return data;
    }

    private Map<String, Object> saveChatUpload(MultipartFile file, String fileExtension, String text, String contentType) throws IOException {
        Path uploadDir = uploadStorageService.getChatDir();
        Files.createDirectories(uploadDir);
        System.out.println("聊天文件上传目录存在: " + uploadDir + ", 可写: " + Files.isWritable(uploadDir));

        boolean isImage = isImageExtension(fileExtension) || (contentType != null && contentType.toLowerCase().startsWith("image/"));
        boolean isAudio = contentType != null && (contentType.toLowerCase().startsWith("audio/") || "video/webm".equalsIgnoreCase(contentType));

        String uniqueFilename = generateUniqueFilename(isImage ? "chat_" : "voice_", fileExtension);
        Path filePath = uploadDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String uploadUrl = uploadStorageService.buildUploadUrl(uniqueFilename);
        String originalFilename = file.getOriginalFilename();

        Map<String, Object> data = new HashMap<>();
        data.put("url", uploadUrl);
        data.put("fileUrl", uploadUrl);
        data.put("path", uploadUrl);
        data.put("src", uploadUrl);
        data.put("filename", uniqueFilename);
        data.put("originalFilename", originalFilename);
        data.put("size", file.getSize());
        data.put("contentType", contentType);
        data.put("mediaType", isImage ? "image" : "voice");
        if (isImage) {
            data.put("imageUrl", uploadUrl);
        }
        if (isAudio) {
            data.put("voiceUrl", uploadUrl);
            data.put("audioUrl", uploadUrl);
            data.put("recordUrl", uploadUrl);
            data.put("mediaUrl", uploadUrl);
        }
        if (text != null) {
            data.put("text", text);
        }

        System.out.println("=== 聊天文件上传成功 ===");
        System.out.println("原始文件名: " + originalFilename);
        System.out.println("保存文件名: " + uniqueFilename);
        System.out.println("文件大小: " + file.getSize() + " bytes");
        System.out.println("聊天文件URL: " + uploadUrl);

        return data;
    }

    // 获取文件扩展名
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return (lastDotIndex > 0) ? filename.substring(lastDotIndex) : "";
    }

    // 检查是否是允许的文件扩展名
    private boolean isAllowedExtension(String extension) {
        return Arrays.asList(ALLOWED_EXTENSIONS).contains(extension);
    }

    private boolean isImageExtension(String extension) {
        return Arrays.asList(IMAGE_EXTENSIONS).contains(extension);
    }

    private boolean isChatUploadExtension(String extension) {
        return Arrays.asList(CHAT_UPLOAD_EXTENSIONS).contains(extension);
    }

    // 生成唯一文件名
    private String generateUniqueFilename(String prefix, String extension) {
        String uuid = UUID.randomUUID().toString();
        return prefix + uuid + extension;
    }

    /**
     * 上传文件并自动上传到WPS
     */
    @PostMapping(value = "/api/file/upload-wps", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFileToWps(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "uploadToWps", defaultValue = "true") boolean uploadToWps) {
        try {
            validateFile(file);

            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename).toLowerCase();
            
            if (!isWpsExtension(fileExtension)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("不支持的文件类型，仅支持: .doc, .docx, .xls, .xlsx, .ppt, .pptx, .pdf, .txt"));
            }

            if (uploadToWps) {
                // 上传到WPS
                Map<String, Object> result = wpsService.uploadToWps(file, userId);
                return ResponseEntity.ok(ApiResponse.success("文件上传成功（WPS）", result));
            } else {
                // 只上传到本地
                Map<String, Object> data = saveFile(file, fileExtension, "file_");
                return ResponseEntity.ok(ApiResponse.success("文件上传成功", data));
            }
        } catch (Exception e) {
            System.out.println("=== WPS文件上传失败 ===");
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("文件上传失败: " + e.getMessage()));
        }
    }

    // 检查是否是WPS支持的文件扩展名
    private boolean isWpsExtension(String extension) {
        return Arrays.asList(WPS_EXTENSIONS).contains(extension);
    }
}
