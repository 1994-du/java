package com.springbootproject.Repository;

import com.springbootproject.Entity.Workbench;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 工作台Repository接口
 */
@Repository
public interface WorkbenchRepository extends JpaRepository<Workbench, Long> {

    /**
     * 按ID升序获取工作台列表
     * @return 工作台列表
     */
    List<Workbench> findAllByOrderByIdAsc();

    /**
     * 按ID升序获取指定工作台列表
     * @param ids 工作台ID集合
     * @return 工作台列表
     */
    List<Workbench> findByIdInOrderByIdAsc(Collection<Long> ids);
}
