package com.springbootproject.Entity;

import jakarta.persistence.*;

/**
 * 组织实体类
 */
@Entity
@Table(name = "organizations")
public class Organization {
    
    /**
     * 组织ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 组织名称
     */
    @Column(name = "name", nullable = false)
    private String name;
    
    /**
     * 组织描述
     */
    @Column(name = "description")
    private String description;
    
    /**
     * 父组织ID（用于多级组织）
     */
    @Column(name = "parent_id")
    private Long parentId;
    
    /**
     * 组织代码
     */
    @Column(name = "code")
    private String code;
    
    /**
     * 组织状态（1: 启用, 0: 禁用）
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