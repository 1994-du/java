package com.springbootproject.Controller;

import com.springbootproject.Entity.Organization;
import com.springbootproject.Model.ApiResponse;
import com.springbootproject.Service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 组织控制器
 */
@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {
    
    @Autowired
    private OrganizationService organizationService;
    
    /**
     * 获取所有组织列表
     * @return 组织列表
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Organization>>> getOrganizations() {
        try {
            List<Organization> organizations = organizationService.getAllOrganizations();
            return ResponseEntity.ok(ApiResponse.success("获取组织列表成功", organizations, (long) organizations.size()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取组织列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取组织树形结构
     * @return 组织树形结构
     */
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrganizationTree() {
        try {
            List<Organization> organizations = organizationService.getAllOrganizations();
            List<Map<String, Object>> tree = organizationService.buildOrganizationTree(organizations, null);
            return ResponseEntity.ok(ApiResponse.success("获取组织树形结构成功", tree));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取组织树形结构失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据ID获取组织详情
     * @param id 组织ID
     * @return 组织详情
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<ApiResponse<Organization>> getOrganizationById(@PathVariable Long id) {
        try {
            Organization organization = organizationService.getOrganizationById(id);
            if (organization == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("组织不存在"));
            }
            return ResponseEntity.ok(ApiResponse.success("获取组织详情成功", organization));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取组织详情失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建组织
     * @param organization 组织对象
     * @return 创建的组织对象
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Organization>> createOrganization(@RequestBody Organization organization) {
        try {
            Organization createdOrg = organizationService.createOrganization(organization);
            return ResponseEntity.ok(ApiResponse.success("创建组织成功", createdOrg));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("创建组织失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新组织
     * @param id 组织ID
     * @param organization 组织对象
     * @return 更新后的组织对象
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<Organization>> updateOrganization(@PathVariable Long id, @RequestBody Organization organization) {
        try {
            Organization updatedOrg = organizationService.updateOrganization(id, organization);
            return ResponseEntity.ok(ApiResponse.success("更新组织成功", updatedOrg));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("更新组织失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除组织
     * @param id 组织ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteOrganization(@PathVariable Long id) {
        try {
            organizationService.deleteOrganization(id);
            return ResponseEntity.ok(ApiResponse.success("删除组织成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("删除组织失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据父组织ID获取子组织列表
     * @param parentId 父组织ID
     * @return 子组织列表
     */
    @GetMapping("/children/{parentId}")
    public ResponseEntity<ApiResponse<List<Organization>>> getChildOrganizations(@PathVariable Long parentId) {
        try {
            List<Organization> organizations = organizationService.getOrganizationsByParentId(parentId);
            return ResponseEntity.ok(ApiResponse.success("获取子组织列表成功", organizations, (long) organizations.size()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取子组织列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据状态获取组织列表
     * @param status 状态（1: 启用, 0: 禁用）
     * @return 组织列表
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<Organization>>> getOrganizationsByStatus(@PathVariable Integer status) {
        try {
            List<Organization> organizations = organizationService.getOrganizationsByStatus(status);
            return ResponseEntity.ok(ApiResponse.success("获取组织列表成功", organizations, (long) organizations.size()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取组织列表失败: " + e.getMessage()));
        }
    }
}