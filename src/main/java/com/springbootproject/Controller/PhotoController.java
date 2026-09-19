package com.springbootproject.Controller;

import com.springbootproject.Entity.Photo;
import com.springbootproject.Entity.User;
import com.springbootproject.Model.ApiResponse;
import com.springbootproject.Repository.UserRepository;
import com.springbootproject.Service.PhotoService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    private static final long MAX_PHOTO_SIZE = 30 * 1024 * 1024; // 30MB
    /** 客户端只会预览图片，上传侧就挡掉非图片，避免下载端拿到打不开的文件 */
    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"};

    private static boolean looksLikeImage(String filename, String contentType) {
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        for (String ext : IMAGE_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private final PhotoService photoService;
    private final UserRepository userRepository;

    public PhotoController(PhotoService photoService, UserRepository userRepository) {
        this.photoService = photoService;
        this.userRepository = userRepository;
    }

    private User requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByUsername(auth.getName());
    }

    private Map<String, Object> toMap(Photo p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("originalFilename", p.getOriginalFilename());
        m.put("size", p.getSize());
        m.put("contentType", p.getContentType());
        // uploadTime 正常非空，但历史/手工插入的数据可能为 null，避免整张列表 500
        m.put("uploadTime", p.getUploadTime() != null ? p.getUploadTime().toString() : "");
        m.put("downloaded", p.isDownloaded());
        return m;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        User user = requireUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("未登录或token无效"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("文件不能为空"));
        }
        if (file.getSize() > MAX_PHOTO_SIZE) {
            return ResponseEntity.badRequest().body(ApiResponse.error("文件大小不能超过30MB"));
        }
        if (!looksLikeImage(file.getOriginalFilename(), file.getContentType())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("仅支持图片文件（jpg/jpeg/png/gif/webp/bmp）"));
        }
        try {
            Photo p = photoService.save(user.getId(), user.getUsername(), file);
            Map<String, Object> m = toMap(p);
            m.put("storedFilename", p.getStoredFilename());
            return ResponseEntity.ok(ApiResponse.success("上传成功", m));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("保存失败: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> list() {
        User user = requireUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("未登录或token无效"));
        }
        List<Map<String, Object>> data = photoService.listByUser(user.getId()).stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("获取成功", data));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> download(@PathVariable Long id) {
        User user = requireUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("未登录或token无效"));
        }
        Optional<Photo> opt = photoService.findByIdAndUser(id, user.getId());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("文件不存在"));
        }
        Photo p = opt.get();
        Path path;
        try {
            path = photoService.resolveFile(p);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("读取文件失败"));
        }
        if (!Files.exists(path)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("文件不存在"));
        }

        Resource resource = new FileSystemResource(path);
        String ct = (p.getContentType() != null && !p.getContentType().isBlank())
                ? p.getContentType() : "application/octet-stream";
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(ct);
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        String dn = URLEncoder.encode(p.getOriginalFilename(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dn + "\"; filename*=UTF-8''" + dn)
                .body(resource);
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody Map<String, Object> body) {
        User user = requireUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("未登录或token无效"));
        }
        List<Long> ids = new ArrayList<>();
        Object idsObj = body.get("ids");
        if (idsObj instanceof List<?>) {
            for (Object o : (List<?>) idsObj) {
                if (o instanceof Number) {
                    ids.add(((Number) o).longValue());
                }
            }
        }
        photoService.deleteByUserAndIds(user.getId(), ids);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}
