package com.springbootproject.Entity;

import jakarta.persistence.*;

/**
 * 工作台实体类
 */
@Entity
@Table(name = "workbenches")
public class Workbench {

    /**
     * 工作台ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 工作台名称
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * 工作台图标
     */
    @Column(name = "icon")
    private String icon;

    /**
     * 子应用跳转链接
     */
    @Column(name = "link")
    private String link;

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
}
