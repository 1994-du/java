package com.springbootproject.Controller;

import com.springbootproject.Model.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicTestController {

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> publicTest() {
        return ResponseEntity.ok(ApiResponse.success("公共测试端点访问成功！", null));
    }
}