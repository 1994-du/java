package com.springbootproject.Repository;

import com.springbootproject.Entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 部门Repository接口
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    /**
     * 根据组织ID查询部门列表
     * @param organizationId 组织ID
     * @return 部门列表
     */
    List<Department> findByOrganizationId(Long organizationId);
    
    /**
     * 根据父部门ID查询部门列表
     * @param parentId 父部门ID
     * @return 部门列表
     */
    List<Department> findByParentId(Long parentId);
    
    /**
     * 根据状态查询部门列表
     * @param status 状态（1: 启用, 0: 禁用）
     * @return 部门列表
     */
    List<Department> findByStatus(Integer status);
    
    /**
     * 根据部门代码查询部门
     * @param code 部门代码
     * @return 部门对象
     */
    Department findByCode(String code);
    
    /**
     * 根据组织ID和父部门ID查询部门列表
     * @param organizationId 组织ID
     * @param parentId 父部门ID
     * @return 部门列表
     */
    List<Department> findByOrganizationIdAndParentId(Long organizationId, Long parentId);
}