package com.springbootproject.Controller;

import com.springbootproject.Entity.Role;
import com.springbootproject.Entity.User;
import com.springbootproject.Model.ApiResponse;
import com.springbootproject.Model.WorkbenchAppResponse;
import com.springbootproject.Model.WorkbenchRequest;
import com.springbootproject.Model.WorkbenchResponse;
import com.springbootproject.Repository.RoleRepository;
import com.springbootproject.Service.WorkbenchService;
import com.springbootproject.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作台控制器
 */
@RestController
@RequestMapping("/api/workbenches")
public class WorkbenchController {

    @Autowired
    private WorkbenchService workbenchService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    /**
     * 获取所有工作台列表（管理员）
     * @return 工作台列表
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<WorkbenchResponse>>> getWorkbenches() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("未登录或token无效"));
        }
        if (!isAdmin(currentUser)) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权限访问工作台管理列表"));
        }

        try {
            List<WorkbenchResponse> workbenches = workbenchService.getAllWorkbenches();
            return ResponseEntity.ok(ApiResponse.success("获取工作台列表成功", workbenches, (long) workbenches.size()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取工作台列表失败: " + e.getMessage()));
        }
    }

    /**
     * 创建工作台（管理员）
     * @param request 工作台请求对象
     * @return 创建后的工作台对象
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<WorkbenchResponse>> createWorkbench(@RequestBody WorkbenchRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("未登录或token无效"));
        }
        if (!isAdmin(currentUser)) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权限创建工作台"));
        }

        try {
            WorkbenchResponse createdWorkbench = workbenchService.createWorkbench(request);
            return ResponseEntity.ok(ApiResponse.success("创建工作台成功", createdWorkbench));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("创建工作台失败: " + e.getMessage()));
        }
    }

    /**
     * 更新工作台（管理员）
     * @param id 工作台ID
     * @param request 工作台请求对象
     * @return 更新后的工作台对象
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<WorkbenchResponse>> updateWorkbench(@PathVariable Long id, @RequestBody WorkbenchRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("未登录或token无效"));
        }
        if (!isAdmin(currentUser)) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权限编辑工作台"));
        }

        try {
            WorkbenchResponse updatedWorkbench = workbenchService.updateWorkbench(id, request);
            return ResponseEntity.ok(ApiResponse.success("更新工作台成功", updatedWorkbench));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("更新工作台失败: " + e.getMessage()));
        }
    }

    /**
     * 删除工作台（管理员）
     * @param id 工作台ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteWorkbench(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("未登录或token无效"));
        }
        if (!isAdmin(currentUser)) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权限删除工作台"));
        }

        try {
            workbenchService.deleteWorkbench(id);
            return ResponseEntity.ok(ApiResponse.success("删除工作台成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("删除工作台失败: " + e.getMessage()));
        }
    }

    /**
     * 根据当前用户获取工作台子应用列表
     * @return 工作台子应用列表
     */
    @GetMapping("/current/list")
    public ResponseEntity<ApiResponse<List<WorkbenchAppResponse>>> getCurrentUserWorkbenches() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("未登录或token无效"));
        }

        try {
            List<WorkbenchAppResponse> workbenches = workbenchService.getWorkbenchAppsByUserId(currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("获取当前用户工作台子应用列表成功", workbenches, (long) workbenches.size()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取当前用户工作台子应用列表失败: " + e.getMessage()));
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        if (!isAuthenticated) {
            return null;
        }
        return userService.findUserByUsername(authentication.getName());
    }

    private boolean isAdmin(User user) {
        if (user == null || user.getRoleId() == null) {
            return false;
        }

        Role adminRole = roleRepository.findByName("管理员");
        if (adminRole != null && user.getRoleId().equals(adminRole.getId())) {
            return true;
        }

        Role superAdminRole = roleRepository.findByName("超级管理员");
        return superAdminRole != null && user.getRoleId().equals(superAdminRole.getId());
    }
}
