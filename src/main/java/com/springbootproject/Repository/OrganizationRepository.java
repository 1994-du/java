package com.springbootproject.Repository;

import com.springbootproject.Entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 组织Repository接口
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    /**
     * 根据父组织ID查询组织列表
     * @param parentId 父组织ID
     * @return 组织列表
     */
    List<Organization> findByParentId(Long parentId);
    
    /**
     * 根据状态查询组织列表
     * @param status 状态（1: 启用, 0: 禁用）
     * @return 组织列表
     */
    List<Organization> findByStatus(Integer status);
    
    /**
     * 根据组织代码查询组织
     * @param code 组织代码
     * @return 组织对象
     */
    Organization findByCode(String code);
}