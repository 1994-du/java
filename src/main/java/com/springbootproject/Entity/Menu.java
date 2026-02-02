package com.springbootproject.Entity;

import jakarta.persistence.*;
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
    private Long menuId;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "path", length = 128)
    private String path;

    @Column(name = "meta", length = 255)
    private String meta;

    @Transient
    private List<Menu> children;

    // Getters and setters
    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public List<Menu> getChildren() {
        return children;
    }

    public void setChildren(List<Menu> children) {
        this.children = children;
    }
}