package com.springbootproject.Controller;

import com.springbootproject.Entity.Department;
import com.springbootproject.Model.ApiResponse;
import com.springbootproject.Service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 部门控制器
 */
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {
    
    @Autowired
    private DepartmentService departmentService;
    
    /**
     * 获取所有部门列表
     * @return 部门列表
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Department>>> getDepartments() {
        try {
            List<Department> departments = departmentService.getAllDepartments();
            return ResponseEntity.ok(ApiResponse.success("获取部门列表成功", departments, (long) departments.size()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取部门列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取部门树形结构
     * @return 部门树形结构
     */
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDepartmentTree() {
        try {
            List<Department> departments = departmentService.getAllDepartments();
            List<Map<String, Object>> tree = departmentService.buildDepartmentTree(departments, null);
            return ResponseEntity.ok(ApiResponse.success("获取部门树形结构成功", tree));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取部门树形结构失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据ID获取部门详情
     * @param id 部门ID
     * @return 部门详情
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<ApiResponse<Department>> getDepartmentById(@PathVariable Long id) {
        try {
            Department department = departmentService.getDepartmentById(id);
            if (department == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("部门不存在"));
            }
            return ResponseEntity.ok(ApiResponse.success("获取部门详情成功", department));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取部门详情失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建部门
     * @param department 部门对象
     * @return 创建的部门对象
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Department>> createDepartment(@RequestBody Department department) {
        try {
            Department createdDept = departmentService.createDepartment(department);
            return ResponseEntity.ok(ApiResponse.success("创建部门成功", createdDept));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("创建部门失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新部门
     * @param id 部门ID
     * @param department 部门对象
     * @return 更新后的部门对象
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<Department>> updateDepartment(@PathVariable Long id, @RequestBody Department department) {
        try {
            Department updatedDept = departmentService.updateDepartment(id, department);
            return ResponseEntity.ok(ApiResponse.success("更新部门成功", updatedDept));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("更新部门失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除部门
     * @param id 部门ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteDepartment(@PathVariable Long id) {
        try {
            departmentService.deleteDepartment(id);
            return ResponseEntity.ok(ApiResponse.success("删除部门成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("删除部门失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据组织ID获取部门列表
     * @param organizationId 组织ID
     * @return 部门列表
     */
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<ApiResponse<List<Department>>> getDepartmentsByOrganizationId(@PathVariable Long organizationId) {
        try {
            List<Department> departments = departmentService.getDepartmentsByOrganizationId(organizationId);
            return ResponseEntity.ok(ApiResponse.success("获取部门列表成功", departments, (long) departments.size()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取部门列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据父部门ID获取子部门列表
     * @param parentId 父部门ID
     * @return 子部门列表
     */
    @GetMapping("/children/{parentId}")
    public ResponseEntity<ApiResponse<List<Department>>> getChildDepartments(@PathVariable Long parentId) {
        try {
            List<Department> departments = departmentService.getDepartmentsByParentId(parentId);
            return ResponseEntity.ok(ApiResponse.success("获取子部门列表成功", departments, (long) departments.size()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取子部门列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据组织ID和父部门ID获取部门列表
     * @param organizationId 组织ID
     * @param parentId 父部门ID
     * @return 部门列表
     */
    @GetMapping("/organization/{organizationId}/parent/{parentId}")
    public ResponseEntity<ApiResponse<List<Department>>> getDepartmentsByOrganizationIdAndParentId(
            @PathVariable Long organizationId, @PathVariable Long parentId) {
        try {
            List<Department> departments = departmentService.getDepartmentsByOrganizationIdAndParentId(organizationId, parentId);
            return ResponseEntity.ok(ApiResponse.success("获取部门列表成功", departments, (long) departments.size()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取部门列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据状态获取部门列表
     * @param status 状态（1: 启用, 0: 禁用）
     * @return 部门列表
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<Department>>> getDepartmentsByStatus(@PathVariable Integer status) {
        try {
            List<Department> departments = departmentService.getDepartmentsByStatus(status);
            return ResponseEntity.ok(ApiResponse.success("获取部门列表成功", departments, (long) departments.size()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取部门列表失败: " + e.getMessage()));
        }
    }
}