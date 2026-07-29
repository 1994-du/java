package com.springbootproject.Repository;

import com.springbootproject.Entity.WorkbenchUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 工作台与用户关联Repository接口
 */
@Repository
public interface WorkbenchUserRepository extends JpaRepository<WorkbenchUser, Long> {

    List<WorkbenchUser> findByWorkbenchId(Long workbenchId);

    List<WorkbenchUser> findByWorkbenchIdIn(Collection<Long> workbenchIds);

    List<WorkbenchUser> findByUserId(Long userId);

    void deleteByWorkbenchId(Long workbenchId);
}
