package com.springbootproject.Controller;

import com.springbootproject.Model.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试控制器，用于验证安全配置是否正确工作
 */
@RestController
public class TestController {

    /**
     * 公开的测试端点，应该可以匿名访问
     */
    @GetMapping("/test-public")
    public ResponseEntity<ApiResponse<String>> testPublic() {
        System.out.println("访问了公开测试端点");
        return ResponseEntity.ok(ApiResponse.success("这是一个公开端点，可以匿名访问", null));
    }
    
    /**
     * WebSocket测试端点，应该可以匿名访问
     */
    @GetMapping("/test-websocket")
    public ResponseEntity<ApiResponse<String>> testWebSocket() {
        System.out.println("访问了WebSocket测试端点");
        return ResponseEntity.ok(ApiResponse.success("WebSocket测试端点，应该可以匿名访问", null));
    }
}