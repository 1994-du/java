package com.springbootproject.Entity;

import jakarta.persistence.*;

/**
 * 组织结构实体类（统一组织和部门）
 */
@Entity
@Table(name = "org_structures")
public class OrgStructure {
    
    /**
     * 结构ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 结构名称
     */
    @Column(name = "name", nullable = false)
    private String name;
    
    /**
     * 结构描述
     */
    @Column(name = "description")
    private String description;
    
    /**
     * 父结构ID（用于多级结构）
     */
    @Column(name = "parent_id")
    private Long parentId;
    
    /**
     * 结构代码
     */
    @Column(name = "code")
    private String code;
    
    /**
     * 结构状态（1: 启用, 0: 禁用）
     */
    @Column(name = "status", nullable = false, columnDefinition = "int default 1")
    private Integer status;
    
    /**
     * 是否为叶子节点（1: 是, 0: 否）
     */
    @Column(name = "is_leaf", nullable = false, columnDefinition = "int default 0")
    private Integer isLeaf;
    
    /**
     * 层级
     */
    @Column(name = "level")
    private Integer level;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public Integer getIsLeaf() {
        return isLeaf;
    }
    
    public void setIsLeaf(Integer isLeaf) {
        this.isLeaf = isLeaf;
    }
    
    public Integer getLevel() {
        return level;
    }
    
    public void setLevel(Integer level) {
        this.level = level;
    }
}