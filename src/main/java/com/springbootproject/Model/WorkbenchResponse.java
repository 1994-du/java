package com.springbootproject.Model;

import java.util.List;

/**
 * 工作台管理响应模型
 */
public class WorkbenchResponse {

    private Long id;
    private String name;
    private String icon;
    private String link;
    private List<WorkbenchAssignedUserResponse> users;

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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public List<WorkbenchAssignedUserResponse> getUsers() {
        return users;
    }

    public void setUsers(List<WorkbenchAssignedUserResponse> users) {
        this.users = users;
    }
}
