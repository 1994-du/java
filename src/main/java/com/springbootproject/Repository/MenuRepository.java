package com.springbootproject.Repository;

import com.springbootproject.Entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {

    /**
     * 根据菜单名称模糊查询
     * @param name 菜单名称关键字
     * @return 匹配的菜单列表
     */
    List<Menu> findByNameContaining(String name);
}