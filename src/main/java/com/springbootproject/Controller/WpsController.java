package com.springbootproject.Controller;

import com.springbootproject.Entity.WpsFile;
import com.springbootproject.Model.ApiResponse;
import com.springbootproject.Service.WpsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/wps")
public class WpsController {

    @Autowired
    private WpsService wpsService;

    /**
     * 上传文件到WPS接口 (供前端调用)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadToWps(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId) {
        try {
            Map<String, Object> result = wpsService.uploadToWps(file, userId);
            return ResponseEntity.ok(ApiResponse.success("上传成功", result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error("上传失败: " + e.getMessage()));
        }
    }

    // ========== WPS v3 回调接口 ==========

    /**
     * WPS回调 - 获取文件信息
     * GET /v3/3rd/files/{file_id}
     */
    @GetMapping("/v3/3rd/files/{file_id}")
    public ResponseEntity<?> getFileInfo(@PathVariable("file_id") String fileId) {
        System.out.println("WPS回调 - 获取文件信息: " + fileId);
        
        Map<String, Object> fileInfo = wpsService.getFileInfo(fileId);
        if (fileInfo == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", -1);
            error.put("msg", "文件不存在");
            return ResponseEntity.badRequest().body(error);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("msg", "success");
        response.put("data", fileInfo);
        
        return ResponseEntity.ok(response);
    }

    /**
     * WPS回调 - 获取文件下载地址
     * GET /v3/3rd/files/{file_id}/download
     */
    @GetMapping("/v3/3rd/files/{file_id}/download")
    public ResponseEntity<?> getFileDownloadUrl(@PathVariable("file_id") String fileId) {
        System.out.println("WPS回调 - 获取文件下载地址: " + fileId);
        
        Map<String, Object> downloadInfo = wpsService.getFileDownloadUrl(fileId);
        if (downloadInfo == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", -1);
            error.put("msg", "文件不存在");
            return ResponseEntity.badRequest().body(error);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("msg", "success");
        response.put("data", downloadInfo);
        
        return ResponseEntity.ok(response);
    }

    /**
     * WPS回调 - 获取文档用户权限
     * GET /v3/3rd/files/{file_id}/permission
     */
    @GetMapping("/v3/3rd/files/{file_id}/permission")
    public ResponseEntity<?> getFilePermission(@PathVariable("file_id") String fileId) {
        System.out.println("WPS回调 - 获取文档用户权限: " + fileId);
        
        Map<String, Object> permission = new HashMap<>();
        permission.put("user_id", "admin");
        permission.put("permission", "write"); // read/write
        permission.put("reusable", true);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("msg", "success");
        response.put("data", permission);
        
        return ResponseEntity.ok(response);
    }

    /**
     * WPS回调 - 文档水印
     * GET /v3/3rd/files/{file_id}/watermark
     */
    @GetMapping("/v3/3rd/files/{file_id}/watermark")
    public ResponseEntity<?> getWatermark(@PathVariable("file_id") String fileId) {
        System.out.println("WPS回调 - 文档水印: " + fileId);
        
        Map<String, Object> watermark = new HashMap<>();
        watermark.put("type", 0); // 0: 关闭, 1: 文字水印, 2: 图片水印
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("msg", "success");
        response.put("data", watermark);
        
        return ResponseEntity.ok(response);
    }

    /**
     * WPS回调 - 事件通知
     * POST /v3/3rd/notify
     */
    @PostMapping("/v3/3rd/notify")
    public ResponseEntity<?> notifyEvent(@RequestBody Map<String, Object> eventData) {
        System.out.println("WPS回调 - 事件通知: " + eventData);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("msg", "success");
        
        return ResponseEntity.ok(response);
    }

    /**
     * WPS回调 - 三阶段保存文件-准备上传阶段
     * GET /v3/3rd/files/{file_id}/upload/prepare
     */
    @GetMapping("/v3/3rd/files/{file_id}/upload/prepare")
    public ResponseEntity<?> prepareUpload(@PathVariable("file_id") String fileId) {
        System.out.println("WPS回调 - 准备上传阶段: " + fileId);
        
        Map<String, Object> prepareInfo = wpsService.prepareUpload(fileId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("msg", "success");
        response.put("data", prepareInfo);
        
        return ResponseEntity.ok(response);
    }

    /**
     * WPS回调 - 三阶段保存文件-获取上传地址
     * POST /v3/3rd/files/{file_id}/upload/address
     */
    @PostMapping("/v3/3rd/files/{file_id}/upload/address")
    public ResponseEntity<?> getUploadAddress(@PathVariable("file_id") String fileId,
                                               @RequestBody Map<String, Object> requestData) {
        System.out.println("WPS回调 - 获取上传地址: " + fileId + ", request: " + requestData);
        
        Map<String, Object> addressInfo = wpsService.getUploadAddress(fileId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("msg", "success");
        response.put("data", addressInfo);
        
        return ResponseEntity.ok(response);
    }

    /**
     * WPS回调 - 获取文件内容 (供下载使用)
     */
    @GetMapping("/callback/file")
    public ResponseEntity<?> getFile(@RequestParam("fileId") String fileId) {
        try {
            WpsFile wpsFile = wpsService.getWpsFileByFileId(fileId);
            if (wpsFile == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("文件不存在"));
            }
            
            Path filePath = Paths.get(wpsFile.getFilePath());
            if (!Files.exists(filePath)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("文件不存在"));
            }
            
            byte[] fileContent = Files.readAllBytes(filePath);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(wpsFile.getContentType() != null ? wpsFile.getContentType() : "application/octet-stream"))
                    .body(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(ApiResponse.error("读取文件失败: " + e.getMessage()));
        }
    }

    /**
     * WPS回调 - 保存文件
     */
    @PostMapping("/callback/save")
    public ResponseEntity<?> saveFile(@RequestParam("fileId") String fileId,
                                      @RequestBody byte[] fileContent) {
        try {
            wpsService.saveFileFromWps(fileId, fileContent);
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileId", fileId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 0);
            response.put("msg", "success");
            response.put("data", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", -1);
            response.put("msg", "保存失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/file/{wpsFileId}")
    public ResponseEntity<?> getFileInfoApi(@PathVariable String wpsFileId) {
        WpsFile wpsFile = wpsService.getWpsFileByFileId(wpsFileId);
        if (wpsFile == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("文件不存在"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("获取成功", wpsFile));
    }
}