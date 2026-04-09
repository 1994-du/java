package com.springbootproject.Entity;

import jakarta.persistence.*;

/**
 * 部门实体类
 */
@Entity
@Table(name = "departments")
public class Department {
    
    /**
     * 部门ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 部门名称
     */
    @Column(name = "name", nullable = false)
    private String name;
    
    /**
     * 部门描述
     */
    @Column(name = "description")
    private String description;
    
    /**
     * 所属组织ID
     */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    
    /**
     * 父部门ID（用于多级部门）
     */
    @Column(name = "parent_id")
    private Long parentId;
    
    /**
     * 部门代码
     */
    @Column(name = "code")
    private String code;
    
    /**
     * 部门状态（1: 启用, 0: 禁用）
     */
    @Column(name = "status", nullable = false, columnDefinition = "int default 1")
    private Integer status;
    
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
    
    public Long getOrganizationId() {
        return organizationId;
    }
    
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
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
}