package com.springbootproject.Controller;

import com.springbootproject.Model.ApiResponse;
import com.springbootproject.Service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/getRoleDict")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRoleDict() {
        return ResponseEntity.ok(ApiResponse.success("获取角色字典成功", roleService.getRoleDictionary()));
    }

    @PostMapping("/getRoles")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestBody(required = false) Map<String, Object> requestBody) {
        String requestKeyword = keyword;
        if (requestBody != null && requestBody.get("keyword") instanceof String bodyKeyword) {
            requestKeyword = bodyKeyword;
        }
        return ResponseEntity.ok(ApiResponse.success("获取角色列表成功",
                roleService.getRoles(page, size, requestKeyword)));
    }

    @PostMapping("/setRole")
    public ResponseEntity<ApiResponse<Object>> setRole(@RequestBody Map<String, Object> roleData) {
        return executeMutation(() -> roleService.updateRole(roleData), "更新角色成功");
    }

    @PostMapping("/addRole")
    public ResponseEntity<ApiResponse<Object>> addRole(@RequestBody Map<String, Object> roleData) {
        return executeMutation(() -> roleService.addRole(roleData), "添加角色成功");
    }

    @PostMapping("/delRole")
    public ResponseEntity<ApiResponse<Object>> delRole(@RequestBody Map<String, Object> roleData) {
        return executeMutation(() -> roleService.deleteRole(roleData), "删除角色成功");
    }

    private ResponseEntity<ApiResponse<Object>> executeMutation(Runnable operation, String successMessage) {
        try {
            operation.run();
            return ResponseEntity.ok(ApiResponse.success(successMessage, null));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(ApiResponse.error(exception.getMessage()));
        }
    }
}
