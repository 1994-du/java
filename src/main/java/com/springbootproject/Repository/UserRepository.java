package com.springbootproject.Repository;

import com.springbootproject.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户数据访问层接口
 * 继承JpaRepository提供基本的CRUD操作
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户对象，如果不存在则返回null
     */
    User findByUsername(String username);
    
    /**
     * 检查用户名是否已存在
     * @param username 用户名
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByUsername(String username);
}