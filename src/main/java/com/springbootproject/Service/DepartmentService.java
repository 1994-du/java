package com.springbootproject.Service;

import com.springbootproject.Entity.Department;
import com.springbootproject.Repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 部门服务实现类
 */
@Service
public class DepartmentService {
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    /**
     * 获取所有部门列表
     * @return 部门列表
     */
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }
    
    /**
     * 根据ID获取部门
     * @param id 部门ID
     * @return 部门对象
     */
    public Department getDepartmentById(Long id) {
        Optional<Department> optional = departmentRepository.findById(id);
        return optional.orElse(null);
    }
    
    /**
     * 根据组织ID获取部门列表
     * @param organizationId 组织ID
     * @return 部门列表
     */
    public List<Department> getDepartmentsByOrganizationId(Long organizationId) {
        return departmentRepository.findByOrganizationId(organizationId);
    }
    
    /**
     * 根据父部门ID获取部门列表
     * @param parentId 父部门ID
     * @return 部门列表
     */
    public List<Department> getDepartmentsByParentId(Long parentId) {
        return departmentRepository.findByParentId(parentId);
    }
    
    /**
     * 根据组织ID和父部门ID获取部门列表
     * @param organizationId 组织ID
     * @param parentId 父部门ID
     * @return 部门列表
     */
    public List<Department> getDepartmentsByOrganizationIdAndParentId(Long organizationId, Long parentId) {
        return departmentRepository.findByOrganizationIdAndParentId(organizationId, parentId);
    }
    
    /**
     * 构建部门树形结构
     * @param departments 部门列表
     * @param parentId 父部门ID
     * @return 部门树形结构
     */
    public List<Map<String, Object>> buildDepartmentTree(List<Department> departments, Long parentId) {
        List<Map<String, Object>> tree = new ArrayList<>();
        
        for (Department dept : departments) {
            if (dept.getParentId() == parentId) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", dept.getId());
                node.put("name", dept.getName());
                node.put("code", dept.getCode());
                node.put("description", dept.getDescription());
                node.put("status", dept.getStatus());
                node.put("parentId", dept.getParentId());
                node.put("organizationId", dept.getOrganizationId());
                
                // 递归构建子部门
                List<Map<String, Object>> children = buildDepartmentTree(departments, dept.getId());
                if (!children.isEmpty()) {
                    node.put("children", children);
                }
                
                tree.add(node);
            }
        }
        
        return tree;
    }
    
    /**
     * 创建部门
     * @param department 部门对象
     * @return 创建的部门对象
     */
    public Department createDepartment(Department department) {
        // 检查部门代码是否已存在
        if (department.getCode() != null && !department.getCode().isEmpty()) {
            Department existingDept = departmentRepository.findByCode(department.getCode());
            if (existingDept != null) {
                throw new RuntimeException("部门代码已存在");
            }
        }
        
        return departmentRepository.save(department);
    }
    
    /**
     * 更新部门
     * @param id 部门ID
     * @param department 部门对象
     * @return 更新后的部门对象
     */
    public Department updateDepartment(Long id, Department department) {
        Optional<Department> optional = departmentRepository.findById(id);
        if (!optional.isPresent()) {
            throw new RuntimeException("部门不存在");
        }
        
        Department existingDept = optional.get();
        
        // 检查部门代码是否已被其他部门使用
        if (department.getCode() != null && !department.getCode().isEmpty() 
                && !department.getCode().equals(existingDept.getCode())) {
            Department codeDept = departmentRepository.findByCode(department.getCode());
            if (codeDept != null) {
                throw new RuntimeException("部门代码已存在");
            }
        }
        
        // 更新部门信息
        existingDept.setName(department.getName());
        existingDept.setDescription(department.getDescription());
        existingDept.setOrganizationId(department.getOrganizationId());
        existingDept.setParentId(department.getParentId());
        existingDept.setCode(department.getCode());
        existingDept.setStatus(department.getStatus());
        
        return departmentRepository.save(existingDept);
    }
    
    /**
     * 删除部门
     * @param id 部门ID
     */
    public void deleteDepartment(Long id) {
        // 检查是否存在子部门
        List<Department> childDepts = departmentRepository.findByParentId(id);
        if (!childDepts.isEmpty()) {
            throw new RuntimeException("存在子部门，无法删除");
        }
        
        departmentRepository.deleteById(id);
    }
    
    /**
     * 根据状态获取部门列表
     * @param status 状态（1: 启用, 0: 禁用）
     * @return 部门列表
     */
    public List<Department> getDepartmentsByStatus(Integer status) {
        return departmentRepository.findByStatus(status);
    }
}