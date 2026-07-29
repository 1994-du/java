package com.springbootproject.Entity;

import jakarta.persistence.*;

/**
 * 工作台与用户关联实体类
 */
@Entity
@Table(
        name = "workbench_users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workbench_id", "user_id"})
)
public class WorkbenchUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workbench_id", nullable = false)
    private Long workbenchId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkbenchId() {
        return workbenchId;
    }

    public void setWorkbenchId(Long workbenchId) {
        this.workbenchId = workbenchId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
