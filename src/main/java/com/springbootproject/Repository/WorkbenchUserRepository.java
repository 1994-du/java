package com.springbootproject.Repository;

import com.springbootproject.Entity.WorkbenchUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from WorkbenchUser wu where wu.workbenchId = :workbenchId")
    void deleteByWorkbenchId(@Param("workbenchId") Long workbenchId);
}
