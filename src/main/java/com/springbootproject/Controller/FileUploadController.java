package com.springbootproject.Controller;

import com.springbootproject.Service.UploadStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
public class FileUploadController {

    // 最大文件大小 (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    // 允许的文件类型
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".pdf", ".doc", ".docx", ".txt"};

    @Autowired
    private UploadStorageService uploadStorageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                response.put("status", "error");
                response.put("message", "文件不能为空");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // 检查文件大小
            if (file.getSize() > MAX_FILE_SIZE) {
                response.put("status", "error");
                response.put("message", "文件大小不能超过5MB");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // 获取文件原始名称和扩展名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                response.put("status", "error");
                response.put("message", "无法获取文件名");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // 检查文件类型
            String fileExtension = getFileExtension(originalFilename).toLowerCase();
            if (!isAllowedExtension(fileExtension)) {
                response.put("status", "error");
                response.put("message", "不支持的文件类型");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            Path uploadDir = uploadStorageService.getFilesDir();
            System.out.println("上传目录存在: " + uploadDir + ", 可写: " + java.nio.file.Files.isWritable(uploadDir));

            // 生成唯一文件名
            String uniqueFilename = generateUniqueFilename(fileExtension);
            Path filePath = uploadDir.resolve(uniqueFilename);

            // 保存文件
            file.transferTo(filePath.toFile());

            // 构建文件访问URL
            String fileUrl = uploadStorageService.buildFileUrl(uniqueFilename);

            // 返回成功响应
            response.put("status", "success");
            response.put("message", "文件上传成功");
            response.put("fileUrl", fileUrl);
            response.put("filename", uniqueFilename);
            response.put("originalFilename", originalFilename);
            response.put("size", file.getSize());

            System.out.println("=== 文件上传成功 ===");
            System.out.println("原始文件名: " + originalFilename);
            System.out.println("保存文件名: " + uniqueFilename);
            System.out.println("文件大小: " + file.getSize() + " bytes");
            System.out.println("文件URL: " + fileUrl);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IOException e) {
            System.out.println("=== 文件上传失败 ===");
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "文件保存失败: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            System.out.println("=== 文件上传发生异常 ===");
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "文件上传失败: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 获取文件扩展名
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return (lastDotIndex > 0) ? filename.substring(lastDotIndex) : "";
    }

    // 检查是否是允许的文件扩展名
    private boolean isAllowedExtension(String extension) {
        for (String allowedExtension : ALLOWED_EXTENSIONS) {
            if (allowedExtension.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    // 生成唯一文件名
    private String generateUniqueFilename(String extension) {
        String uuid = UUID.randomUUID().toString();
        return "file_" + uuid + extension;
    }
}
