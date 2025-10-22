package com.springbootproject.Model;

/**
 * 密码重置请求数据模型
 */
public class ResetPasswordRequest {
    private String username;
    private String newPassword;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}