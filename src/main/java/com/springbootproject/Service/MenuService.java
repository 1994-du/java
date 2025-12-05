package com.springbootproject.Service;

import com.springbootproject.Entity.Menu;
import com.springbootproject.Repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;

    /**
     * 获取所有菜单
     * @return 菜单列表
     */
    public List<Menu> getAllMenus() {
        return menuRepository.findAll();
    }

    /**
     * 获取所有正常状态的菜单
     * @return 正常状态的菜单列表
     */
    public List<Menu> getActiveMenus() {
        return menuRepository.findByStatus(1);
    }

    /**
     * 获取所有显示的菜单
     * @return 显示的菜单列表
     */
    public List<Menu> getVisibleMenus() {
        return menuRepository.findByVisible(1);
    }

    /**
     * 根据ID获取菜单
     * @param menuId 菜单ID
     * @return 菜单对象
     */
    public Optional<Menu> getMenuById(Long menuId) {
        // 添加空值检查
        if (menuId == null) {
            return Optional.empty();
        }
        return menuRepository.findById(menuId);
    }

    /**
     * 保存菜单
     * @param menu 菜单对象
     * @return 保存后的菜单对象
     */
    public Menu saveMenu(Menu menu) {
        // 添加空值检查
        if (menu == null) {
            throw new IllegalArgumentException("Menu cannot be null");
        }
        return menuRepository.save(menu);
    }

    /**
     * 批量保存菜单
     * @param menus 菜单列表
     * @return 保存后的菜单列表
     */
    public List<Menu> saveMenus(List<Menu> menus) {
        // 添加空值检查
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        // 过滤掉null菜单
        List<Menu> validMenus = menus.stream().filter(Objects::nonNull).collect(Collectors.toList());
        return menuRepository.saveAll(validMenus);
    }

    /**
     * 删除菜单
     * @param menuId 菜单ID
     */
    public void deleteMenu(Long menuId) {
        // 添加空值检查
        if (menuId == null) {
            throw new IllegalArgumentException("Menu ID cannot be null");
        }
        menuRepository.deleteById(menuId);
    }

    /**
     * 获取树形结构的菜单
     * @return 树形结构的菜单列表
     */
    public List<Menu> getMenuTree() {
        List<Menu> allMenus = getActiveMenus();
        if (allMenus == null) {
            allMenus = new ArrayList<>();
        }
        return buildMenuTree(allMenus, 0L);
    }

    /**
     * 获取树形结构的显示菜单（用于前端侧边栏）
     * @return 树形结构的显示菜单列表
     */
    public List<Menu> getVisibleMenuTree() {
        List<Menu> menus = menuRepository.findAll();
        // 手动过滤可见且状态正常的菜单，添加menu空值检查
        List<Menu> visibleMenus = menus.stream()
                .filter(menu -> menu != null && menu.getVisible() != null && menu.getVisible() == 1 && menu.getStatus() != null && menu.getStatus() == 1)
                .collect(Collectors.toList());
        return buildMenuTree(visibleMenus, 0L);
    }

    /**
     * 根据父菜单ID查询子菜单
     * @param parentId 父菜单ID
     * @return 子菜单列表
     */
    public List<Menu> getChildMenus(Long parentId) {
        // 添加空值检查
        if (parentId == null) {
            parentId = 0L;
        }
        return menuRepository.findByParentIdOrderBySortAsc(parentId);
    }

    /**
     * 构建菜单树形结构
     * @param menus 菜单列表
     * @param parentId 父菜单ID
     * @return 树形结构的菜单列表
     */
    private List<Menu> buildMenuTree(List<Menu> menus, Long parentId) {
        // 添加空值检查
        if (menus == null) {
            return new ArrayList<>();
        }
        
        Map<Long, List<Menu>> menuMap = new HashMap<>();
        
        // 按父ID分组，使用stream API并过滤null值
        menuMap = menus.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(menu -> {
                    Long pId = menu.getParentId();
                    return pId != null ? pId : 0L;
                }));
        
        // 递归构建树
        return buildTreeRecursive(menuMap, parentId);
    }

    /**
     * 递归构建树
     * @param menuMap 按父ID分组的菜单映射
     * @param parentId 父菜单ID
     * @return 树形结构的菜单列表
     */
    private List<Menu> buildTreeRecursive(Map<Long, List<Menu>> menuMap, Long parentId) {
        // 添加空值检查
        if (menuMap == null) {
            return new ArrayList<>();
        }
        
        List<Menu> children = menuMap.getOrDefault(parentId, new ArrayList<>());
        
        if (children.isEmpty()) {
            return children;
        }
        
        // 对子菜单按排序值排序
        children.sort((m1, m2) -> {
            Integer sort1 = m1 != null && m1.getSort() != null ? m1.getSort() : 0;
            Integer sort2 = m2 != null && m2.getSort() != null ? m2.getSort() : 0;
            return sort1.compareTo(sort2);
        });
        
        // 递归构建子菜单
        for (Menu menu : children) {
            if (menu != null && menu.getMenuId() != null) {
                menu.setChildren(buildTreeRecursive(menuMap, menu.getMenuId()));
            }
        }
        
        return children;
    }

    /**
     * 根据菜单类型获取菜单
     * @param type 菜单类型：1-目录，2-菜单，3-按钮
     * @return 菜单列表
     */
    public List<Menu> getMenusByType(Integer type) {
        // 添加空值检查
        if (type == null) {
            return new ArrayList<>();
        }
        return menuRepository.findByType(type);
    }

    /**
     * 搜索菜单
     * @param keyword 搜索关键字
     * @return 匹配的菜单列表
     */
    public List<Menu> searchMenus(String keyword) {
        // 添加空值检查
        if (keyword == null) {
            return new ArrayList<>();
        }
        return menuRepository.findByNameContaining(keyword);
    }
}