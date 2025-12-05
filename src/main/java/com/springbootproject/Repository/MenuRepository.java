package com.springbootproject.Repository;

import com.springbootproject.Entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {

    /**
     * 根据父菜单ID查询子菜单
     * @param parentId 父菜单ID
     * @return 子菜单列表
     */
    List<Menu> findByParentId(Long parentId);

    /**
     * 根据状态查询菜单
     * @param status 状态：0-禁用，1-正常
     * @return 菜单列表
     */
    List<Menu> findByStatus(Integer status);

    /**
     * 根据是否显示查询菜单
     * @param visible 是否显示：0-隐藏，1-显示
     * @return 菜单列表
     */
    List<Menu> findByVisible(Integer visible);

    /**
     * 根据类型查询菜单
     * @param type 类型：1-目录，2-菜单，3-按钮
     * @return 菜单列表
     */
    List<Menu> findByType(Integer type);

    /**
     * 根据排序值升序获取菜单列表
     * @return 排序后的菜单列表
     */
    List<Menu> findAllByOrderBySortAsc();

    /**
     * 根据父菜单ID和排序值升序获取菜单列表
     * @param parentId 父菜单ID
     * @return 排序后的子菜单列表
     */
    List<Menu> findByParentIdOrderBySortAsc(Long parentId);

    /**
     * 根据菜单名称模糊查询
     * @param name 菜单名称关键字
     * @return 匹配的菜单列表
     */
    List<Menu> findByNameContaining(String name);
}