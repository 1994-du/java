package com.springbootproject.Repository;

import com.springbootproject.Entity.OrgStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 组织结构Repository接口
 */
@Repository
public interface OrgStructureRepository extends JpaRepository<OrgStructure, Long> {
    
    /**
     * 根据父结构ID查询组织结构列表
     * @param parentId 父结构ID
     * @return 组织结构列表
     */
    List<OrgStructure> findByParentId(Long parentId);
    
    /**
     * 根据状态查询组织结构列表
     * @param status 状态（1: 启用, 0: 禁用）
     * @return 组织结构列表
     */
    List<OrgStructure> findByStatus(Integer status);
    
    /**
     * 根据结构代码查询组织结构
     * @param code 结构代码
     * @return 组织结构对象
     */
    OrgStructure findByCode(String code);
    
    /**
     * 根据是否为叶子节点查询组织结构列表
     * @param isLeaf 是否为叶子节点（1: 是, 0: 否）
     * @return 组织结构列表
     */
    List<OrgStructure> findByIsLeaf(Integer isLeaf);
    
    /**
     * 根据层级查询组织结构列表
     * @param level 层级
     * @return 组织结构列表
     */
    List<OrgStructure> findByLevel(Integer level);
}