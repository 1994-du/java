package com.springbootproject.Entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色实体类
 * 用于存储用户角色信息
 */
@Entity
@Table(name = "sys_roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;  // 角色名称
    private String menus;  // 菜单ID列表，JSON格式
    private String roleDesc;  // 角色描述

    // Getters and setters
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

    public String getMenus() {
        return menus;
    }

    public void setMenus(String menus) {
        this.menus = menus;
    }

    public String getRoleDesc() {
        return roleDesc;
    }

    public void setRoleDesc(String roleDesc) {
        this.roleDesc = roleDesc;
    }
}