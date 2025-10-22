package com.springbootproject.Model;

/**
 * 添加用户请求DTO类
 * 用于接收JSON格式的用户创建请求
 */
public class AddUserRequest {
    private String username;
    private Long roleId;     // 可选
    private String avatar;   // 可选，用于接收头像的字符串

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}