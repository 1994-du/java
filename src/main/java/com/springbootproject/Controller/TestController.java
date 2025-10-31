package com.springbootproject.Controller;

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
    public String testPublic() {
        System.out.println("访问了公开测试端点");
        return "{\"success\":true,\"message\":\"这是一个公开端点，可以匿名访问\"}";
    }
    
    /**
     * WebSocket测试端点，应该可以匿名访问
     */
    @GetMapping("/test-websocket")
    public String testWebSocket() {
        System.out.println("访问了WebSocket测试端点");
        return "{\"success\":true,\"message\":\"WebSocket测试端点，应该可以匿名访问\"}";
    }
}