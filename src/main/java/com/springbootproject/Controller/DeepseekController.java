package com.springbootproject.Controller;

import com.springbootproject.Model.ApiResponse;
import com.springbootproject.Service.DeepseekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/deepseek")
public class DeepseekController {

    @Autowired
    private DeepseekService deepseekService;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> request) {
        try {
            String message = (String) request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("消息内容不能为空"));
            }

            String response = deepseekService.callDeepseek(message);
            return ResponseEntity.ok(ApiResponse.success("调用成功", response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("调用失败: " + e.getMessage()));
        }
    }
}