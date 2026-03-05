package com.springbootproject.Entity;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * 菜单实体类
 * 用于存储系统菜单信息
 */
@Entity
@Table(name = "sys_menus")
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "path", length = 128)
    private String path;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "component", length = 255)
    private String component;

    @Column(name = "meta", length = 255)
    private String meta;

    @Column(name = "icon", length = 64)
    private String icon;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "sort", nullable = false)
    private Integer sort;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @Transient
    private List<Menu> children;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public List<Menu> getChildren() {
        return children;
    }

    public void setChildren(List<Menu> children) {
        this.children = children;
    }

    // 兼容旧代码的方法
    @com.fasterxml.jackson.annotation.JsonIgnore
    public List<Menu> getMenus() {
        return children;
    }

    public void setMenus(List<Menu> menus) {
        this.children = menus;
    }
}