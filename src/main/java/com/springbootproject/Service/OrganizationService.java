package com.springbootproject.Service;

import com.springbootproject.Entity.Organization;
import com.springbootproject.Repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 组织服务实现类
 */
@Service
public class OrganizationService {
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    /**
     * 获取所有组织列表
     * @return 组织列表
     */
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }
    
    /**
     * 根据ID获取组织
     * @param id 组织ID
     * @return 组织对象
     */
    public Organization getOrganizationById(Long id) {
        Optional<Organization> optional = organizationRepository.findById(id);
        return optional.orElse(null);
    }
    
    /**
     * 根据父组织ID获取组织列表
     * @param parentId 父组织ID
     * @return 组织列表
     */
    public List<Organization> getOrganizationsByParentId(Long parentId) {
        return organizationRepository.findByParentId(parentId);
    }
    
    /**
     * 构建组织树形结构
     * @param organizations 组织列表
     * @param parentId 父组织ID
     * @return 组织树形结构
     */
    public List<Map<String, Object>> buildOrganizationTree(List<Organization> organizations, Long parentId) {
        List<Map<String, Object>> tree = new ArrayList<>();
        
        for (Organization org : organizations) {
            if (org.getParentId() == parentId) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", org.getId());
                node.put("name", org.getName());
                node.put("code", org.getCode());
                node.put("description", org.getDescription());
                node.put("status", org.getStatus());
                node.put("parentId", org.getParentId());
                
                // 递归构建子组织
                List<Map<String, Object>> children = buildOrganizationTree(organizations, org.getId());
                if (!children.isEmpty()) {
                    node.put("children", children);
                }
                
                tree.add(node);
            }
        }
        
        return tree;
    }
    
    /**
     * 创建组织
     * @param organization 组织对象
     * @return 创建的组织对象
     */
    public Organization createOrganization(Organization organization) {
        // 检查组织代码是否已存在
        if (organization.getCode() != null && !organization.getCode().isEmpty()) {
            Organization existingOrg = organizationRepository.findByCode(organization.getCode());
            if (existingOrg != null) {
                throw new RuntimeException("组织代码已存在");
            }
        }
        
        return organizationRepository.save(organization);
    }
    
    /**
     * 更新组织
     * @param id 组织ID
     * @param organization 组织对象
     * @return 更新后的组织对象
     */
    public Organization updateOrganization(Long id, Organization organization) {
        Optional<Organization> optional = organizationRepository.findById(id);
        if (!optional.isPresent()) {
            throw new RuntimeException("组织不存在");
        }
        
        Organization existingOrg = optional.get();
        
        // 检查组织代码是否已被其他组织使用
        if (organization.getCode() != null && !organization.getCode().isEmpty() 
                && !organization.getCode().equals(existingOrg.getCode())) {
            Organization codeOrg = organizationRepository.findByCode(organization.getCode());
            if (codeOrg != null) {
                throw new RuntimeException("组织代码已存在");
            }
        }
        
        // 更新组织信息
        existingOrg.setName(organization.getName());
        existingOrg.setDescription(organization.getDescription());
        existingOrg.setParentId(organization.getParentId());
        existingOrg.setCode(organization.getCode());
        existingOrg.setStatus(organization.getStatus());
        
        return organizationRepository.save(existingOrg);
    }
    
    /**
     * 删除组织
     * @param id 组织ID
     */
    public void deleteOrganization(Long id) {
        // 检查是否存在子组织
        List<Organization> childOrgs = organizationRepository.findByParentId(id);
        if (!childOrgs.isEmpty()) {
            throw new RuntimeException("存在子组织，无法删除");
        }
        
        organizationRepository.deleteById(id);
    }
    
    /**
     * 根据状态获取组织列表
     * @param status 状态（1: 启用, 0: 禁用）
     * @return 组织列表
     */
    public List<Organization> getOrganizationsByStatus(Integer status) {
        return organizationRepository.findByStatus(status);
    }
}