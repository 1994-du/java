package com.springbootproject.Service;

import com.springbootproject.Entity.WpsFile;
import com.springbootproject.Repository.WpsFileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

@Service
public class WpsService {

    @Autowired
    private WpsFileRepository wpsFileRepository;

    @Autowired
    private UploadStorageService uploadStorageService;

    @Value("${wps.app.id:}")
    private String wpsAppId;

    @Value("${wps.app.secret:}")
    private String wpsAppSecret;

    @Value("${wps.api.base.url:https://open.wps.cn}")
    private String wpsApiBaseUrl;

    @Value("${wps.callback.domain:}")
    private String wpsCallbackDomain;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WpsService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 上传文件到WPS并获取fileId (WPS v3 API)
     */
    public Map<String, Object> uploadToWps(MultipartFile file, Long userId) throws Exception {
        // 1. 先保存文件到本地
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = "wps_" + UUID.randomUUID().toString() + fileExtension;
        
        Path savePath = uploadStorageService.getFilesDir().resolve(uniqueFilename);
        Files.copy(file.getInputStream(), savePath);
        
        // 2. 生成wpsFileId (使用本地ID，因为WPS v3需要先有fileId)
        String wpsFileId = "wps_" + UUID.randomUUID().toString();
        
        // 3. 保存到数据库
        WpsFile wpsFile = new WpsFile();
        wpsFile.setWpsFileId(wpsFileId);
        wpsFile.setOriginalFilename(originalFilename);
        wpsFile.setFilePath(savePath.toString());
        wpsFile.setFileUrl(uploadStorageService.buildFileUrl(uniqueFilename));
        wpsFile.setContentType(file.getContentType());
        wpsFile.setFileSize(file.getSize());
        wpsFile.setUserId(userId);
        
        wpsFile = wpsFileRepository.save(wpsFile);
        
        // 4. 生成WPS预览URL
        String previewUrl = generateWpsPreviewUrl(wpsFileId, originalFilename);
        
        // 5. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("wpsFileId", wpsFileId);
        result.put("fileId", wpsFile.getId());
        result.put("fileUrl", wpsFile.getFileUrl());
        result.put("originalFilename", originalFilename);
        result.put("previewUrl", previewUrl);
        
        return result;
    }

    /**
     * 生成WPS在线预览URL
     */
    public String generateWpsPreviewUrl(String fileId, String filename) {
        // WPS v3在线预览URL格式
        // 实际使用时需要根据WPS文档生成签名
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String sign = generateWpsSign(fileId, timestamp);
        
        StringBuilder url = new StringBuilder(wpsApiBaseUrl);
        url.append("/office/p/").append(wpsAppId);
        url.append("?file_id=").append(fileId);
        url.append("&_w_tokentype=1");
        url.append("&_w_userid=admin");
        url.append("&_w_timestamp=").append(timestamp);
        url.append("&_w_signature=").append(sign);
        
        if (filename != null) {
            try {
                url.append("&file_name=").append(URLEncoder.encode(filename, StandardCharsets.UTF_8));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return url.toString();
    }

    /**
     * 生成WPS签名
     */
    private String generateWpsSign(String fileId, String timestamp) {
        try {
            // WPS签名算法：md5(app_secret + file_id + timestamp)
            String signStr = wpsAppSecret + fileId + timestamp;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(signStr.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 获取文件信息 (WPS回调接口 - GET /v3/3rd/files/${file_id})
     */
    public Map<String, Object> getFileInfo(String fileId) {
        WpsFile wpsFile = wpsFileRepository.findByWpsFileId(fileId).orElse(null);
        if (wpsFile == null) {
            return null;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("file_id", wpsFile.getWpsFileId());
        result.put("file_name", wpsFile.getOriginalFilename());
        result.put("file_size", wpsFile.getFileSize());
        result.put("version", 1);
        result.put("create_time", wpsFile.getCreatedAt() != null ? wpsFile.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : System.currentTimeMillis() / 1000);
        result.put("modify_time", wpsFile.getUpdatedAt() != null ? wpsFile.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : System.currentTimeMillis() / 1000);
        
        return result;
    }

    /**
     * 获取文件下载地址 (WPS回调接口 - GET /v3/3rd/files/${file_id}/download)
     */
    public Map<String, Object> getFileDownloadUrl(String fileId) {
        WpsFile wpsFile = wpsFileRepository.findByWpsFileId(fileId).orElse(null);
        if (wpsFile == null) {
            return null;
        }
        
        // 这里应该生成一个临时下载URL
        String downloadUrl = wpsCallbackDomain != null && !wpsCallbackDomain.isEmpty() 
                ? wpsCallbackDomain + "/wps/callback/file?fileId=" + fileId
                : wpsFile.getFileUrl();
        
        Map<String, Object> result = new HashMap<>();
        result.put("url", downloadUrl);
        
        return result;
    }

    /**
     * 准备上传阶段 (WPS回调接口 - GET /v3/3rd/files/${file_id}/upload/prepare)
     */
    public Map<String, Object> prepareUpload(String fileId) {
        Map<String, Object> result = new HashMap<>();
        result.put("digest_type", "md5");
        result.put("digest", ""); // 可以计算文件MD5
        return result;
    }

    /**
     * 获取上传地址 (WPS回调接口 - POST /v3/3rd/files/${file_id}/upload/address)
     */
    public Map<String, Object> getUploadAddress(String fileId) {
        Map<String, Object> result = new HashMap<>();
        result.put("put_url", wpsCallbackDomain != null && !wpsCallbackDomain.isEmpty() 
                ? wpsCallbackDomain + "/wps/callback/save?fileId=" + fileId
                : "");
        result.put("put_type", "post");
        return result;
    }

    /**
     * 根据wpsFileId获取文件
     */
    public WpsFile getWpsFileByFileId(String wpsFileId) {
        return wpsFileRepository.findByWpsFileId(wpsFileId).orElse(null);
    }

    /**
     * 保存从WPS回来的文件
     */
    public void saveFileFromWps(String wpsFileId, byte[] fileContent) throws Exception {
        WpsFile wpsFile = wpsFileRepository.findByWpsFileId(wpsFileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        
        // 保存文件
        Path filePath = Paths.get(wpsFile.getFilePath());
        Files.write(filePath, fileContent);
        
        wpsFileRepository.save(wpsFile);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}