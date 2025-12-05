package com.springbootproject.Repository;

import com.springbootproject.Entity.UserRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户路由数据访问层接口
 * 继承JpaRepository提供基本的CRUD操作
 */
@Repository
public interface UserRouteRepository extends JpaRepository<UserRoute, Long> {
    
    /**
     * 根据用户ID查找该用户的所有路由
     * @param userId 用户ID
     * @return 用户路由列表
     */
    List<UserRoute> findByUserId(Long userId);
    
    /**
     * 根据用户ID和路由是否可见查找路由
     * @param userId 用户ID
     * @param visible 是否可见
     * @return 用户可见/不可见路由列表
     */
    List<UserRoute> findByUserIdAndVisible(Long userId, Boolean visible);
    
    /**
     * 删除用户的所有路由
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
}