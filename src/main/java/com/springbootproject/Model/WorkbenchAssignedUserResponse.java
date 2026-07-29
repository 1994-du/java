package com.springbootproject.Model;

/**
 * 工作台关联用户响应模型
 */
public class WorkbenchAssignedUserResponse {

    private Long id;
    private String username;

    public WorkbenchAssignedUserResponse() {
    }

    public WorkbenchAssignedUserResponse(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
