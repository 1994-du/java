package com.springbootproject.Service;

import com.springbootproject.Entity.OrgStructure;
import com.springbootproject.Repository.OrgStructureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 组织结构服务实现类
 */
@Service
@Transactional
public class OrgStructureService {
    
    @Autowired
    private OrgStructureRepository orgStructureRepository;
    
    /**
     * 获取所有组织结构列表
     * @return 组织结构列表
     */
    public List<OrgStructure> getAllOrgStructures() {
        return orgStructureRepository.findAll();
    }
    
    /**
     * 根据ID获取组织结构
     * @param id 结构ID
     * @return 组织结构对象
     */
    public OrgStructure getOrgStructureById(Long id) {
        Optional<OrgStructure> optional = orgStructureRepository.findById(id);
        return optional.orElse(null);
    }
    
    /**
     * 根据父结构ID获取组织结构列表
     * @param parentId 父结构ID
     * @return 组织结构列表
     */
    public List<OrgStructure> getOrgStructuresByParentId(Long parentId) {
        return orgStructureRepository.findByParentId(parentId);
    }
    
    /**
     * 构建组织结构树形结构
     * @param structures 组织结构列表
     * @param parentId 父结构ID
     * @return 组织结构树形结构
     */
    public List<Map<String, Object>> buildOrgStructureTree(List<OrgStructure> structures, Long parentId) {
        List<Map<String, Object>> tree = new ArrayList<>();
        
        for (OrgStructure structure : structures) {
            // 支持 null 或 0 作为根节点的 parentId
            Long structureParentId = structure.getParentId();
            boolean isRootNode = (parentId == null && (structureParentId == null || structureParentId == 0))
                    || (parentId != null && Objects.equals(structureParentId, parentId));
            if (isRootNode) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", structure.getId());
                node.put("name", structure.getName());
                node.put("code", structure.getCode());
                node.put("description", structure.getDescription());
                node.put("status", structure.getStatus());
                node.put("parentId", structure.getParentId());
                node.put("isLeaf", structure.getIsLeaf());
                node.put("level", structure.getLevel());
                
                // 递归构建子结构
                List<Map<String, Object>> children = buildOrgStructureTree(structures, structure.getId());
                if (!children.isEmpty()) {
                    node.put("children", children);
                }
                
                tree.add(node);
            }
        }
        
        return tree;
    }
    
    /**
     * 创建组织结构
     * @param structure 组织结构对象
     * @return 创建的组织结构对象
     */
    public OrgStructure createOrgStructure(OrgStructure structure) {
        // 检查结构代码是否已存在
        if (structure.getCode() != null && !structure.getCode().isEmpty()) {
            OrgStructure existingStructure = orgStructureRepository.findByCode(structure.getCode());
            if (existingStructure != null) {
                throw new RuntimeException("结构代码已存在");
            }
        }
        
        // 计算层级
        if (structure.getParentId() == null) {
            structure.setLevel(1);
        } else {
            OrgStructure parentStructure = orgStructureRepository.findById(structure.getParentId()).orElse(null);
            if (parentStructure != null) {
                structure.setLevel(parentStructure.getLevel() + 1);
                // 如果父结构是叶子节点，将其改为非叶子节点
                if (parentStructure.getIsLeaf() == 1) {
                    parentStructure.setIsLeaf(0);
                    orgStructureRepository.save(parentStructure);
                }
            } else {
                structure.setLevel(1);
            }
        }
        
        // 设置是否为叶子节点（默认为叶子节点）
        if (structure.getIsLeaf() == null) {
            structure.setIsLeaf(1);
        }
        
        return orgStructureRepository.save(structure);
    }
    
    /**
     * 更新组织结构
     * @param id 结构ID
     * @param structure 组织结构对象
     * @return 更新后的组织结构对象
     */
    public OrgStructure updateOrgStructure(Long id, OrgStructure structure) {
        Optional<OrgStructure> optional = orgStructureRepository.findById(id);
        if (!optional.isPresent()) {
            throw new RuntimeException("组织结构不存在");
        }
        
        OrgStructure existingStructure = optional.get();
        
        // 检查结构代码是否已被其他结构使用
        if (structure.getCode() != null && !structure.getCode().isEmpty() 
                && !structure.getCode().equals(existingStructure.getCode())) {
            OrgStructure codeStructure = orgStructureRepository.findByCode(structure.getCode());
            if (codeStructure != null) {
                throw new RuntimeException("结构代码已存在");
            }
        }
        
        // 更新结构信息
        existingStructure.setName(structure.getName());
        existingStructure.setDescription(structure.getDescription());
        existingStructure.setParentId(structure.getParentId());
        existingStructure.setCode(structure.getCode());
        existingStructure.setStatus(structure.getStatus());
        existingStructure.setIsLeaf(structure.getIsLeaf());
        
        // 重新计算层级
        if (existingStructure.getParentId() == null) {
            existingStructure.setLevel(1);
        } else {
            OrgStructure parentStructure = orgStructureRepository.findById(existingStructure.getParentId()).orElse(null);
            if (parentStructure != null) {
                existingStructure.setLevel(parentStructure.getLevel() + 1);
                // 如果父结构是叶子节点，将其改为非叶子节点
                if (parentStructure.getIsLeaf() == 1) {
                    parentStructure.setIsLeaf(0);
                    orgStructureRepository.save(parentStructure);
                }
            } else {
                existingStructure.setLevel(1);
            }
        }
        
        // 检查是否有子结构
        List<OrgStructure> childStructures = orgStructureRepository.findByParentId(id);
        if (!childStructures.isEmpty() && existingStructure.getIsLeaf() == 1) {
            existingStructure.setIsLeaf(0);
        }
        
        return orgStructureRepository.save(existingStructure);
    }
    
    /**
     * 删除组织结构
     * @param id 结构ID
     */
    public void deleteOrgStructure(Long id) {
        // 检查是否存在子结构
        List<OrgStructure> childStructures = orgStructureRepository.findByParentId(id);
        if (!childStructures.isEmpty()) {
            throw new RuntimeException("存在子结构，无法删除");
        }
        
        // 删除结构
        orgStructureRepository.deleteById(id);
        
        // 检查父结构是否变为叶子节点
        OrgStructure structure = orgStructureRepository.findById(id).orElse(null);
        if (structure != null && structure.getParentId() != null) {
            OrgStructure parentStructure = orgStructureRepository.findById(structure.getParentId()).orElse(null);
            if (parentStructure != null) {
                List<OrgStructure> parentChildStructures = orgStructureRepository.findByParentId(parentStructure.getId());
                if (parentChildStructures.isEmpty()) {
                    parentStructure.setIsLeaf(1);
                    orgStructureRepository.save(parentStructure);
                }
            }
        }
    }
    
    /**
     * 根据状态获取组织结构列表
     * @param status 状态（1: 启用, 0: 禁用）
     * @return 组织结构列表
     */
    public List<OrgStructure> getOrgStructuresByStatus(Integer status) {
        return orgStructureRepository.findByStatus(status);
    }
    
    /**
     * 根据是否为叶子节点获取组织结构列表
     * @param isLeaf 是否为叶子节点（1: 是, 0: 否）
     * @return 组织结构列表
     */
    public List<OrgStructure> getOrgStructuresByIsLeaf(Integer isLeaf) {
        return orgStructureRepository.findByIsLeaf(isLeaf);
    }
    
    /**
     * 根据层级获取组织结构列表
     * @param level 层级
     * @return 组织结构列表
     */
    public List<OrgStructure> getOrgStructuresByLevel(Integer level) {
        return orgStructureRepository.findByLevel(level);
    }
    
    /**
     * 获取组织结构的完整路径
     * @param id 结构ID
     * @return 完整路径
     */
    public String getOrgStructurePath(Long id) {
        StringBuilder path = new StringBuilder();
        OrgStructure structure = orgStructureRepository.findById(id).orElse(null);
        
        while (structure != null) {
            if (path.length() > 0) {
                path.insert(0, "/");
            }
            path.insert(0, structure.getName());
            structure = structure.getParentId() != null ? orgStructureRepository.findById(structure.getParentId()).orElse(null) : null;
        }
        
        return path.toString();
    }
}