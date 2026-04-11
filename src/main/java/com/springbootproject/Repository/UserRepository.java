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
    
    User findByUsername(String username);
    
    boolean existsByUsername(String username);
    
    java.util.List<User> findByUsernameContaining(String username);
    
    org.springframework.data.domain.Page<User> findByUsernameContaining(String username, org.springframework.data.domain.Pageable pageable);
}