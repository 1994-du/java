package com.springbootproject.Entity;

import jakarta.persistence.*;
import java.util.List;

/**
 * 用户路由实体类
 * 用于存储用户可访问的路由信息
 */
@Entity
@Table(name = "user_route")
public class UserRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;  // 用户ID，关联到User表
    private String routeName;  // 路由名称
    private String routePath;  // 路由路径
    private String component;  // 组件路径
    private String icon;  // 图标
    private Integer orderNum;  // 排序号
    private Long parentId;  // 父路由ID（用于多级菜单）
    private String permission;  // 权限标识
    private Boolean visible;  // 是否可见
    
    @Transient  // 非持久化字段，用于树形结构构建
    private List<UserRoute> children;  // 子路由列表

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
    
    public List<UserRoute> getChildren() {
        return children;
    }
    
    public void setChildren(List<UserRoute> children) {
        this.children = children;
    }
}