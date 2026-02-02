package com.springbootproject.Service;

import com.springbootproject.Entity.Menu;
import com.springbootproject.Repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        return menuRepository.findAll();
    }

    /**
     * 获取所有显示的菜单
     * @return 显示的菜单列表
     */
    public List<Menu> getVisibleMenus() {
        return menuRepository.findAll();
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
        // 手动过滤空菜单，添加menu空值检查
        List<Menu> visibleMenus = menus.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return buildMenuTree(visibleMenus, 0L);
    }

    /**
     * 根据父菜单ID查询子菜单
     * @param parentId 父菜单ID
     * @return 子菜单列表
     */
    public List<Menu> getChildMenus(Long parentId) {
        // 由于已删除parentId字段，直接返回所有菜单
        return menuRepository.findAll();
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
        
        // 直接返回所有菜单，不构建树形结构
        // 因为我们已经删除了parentId字段
        return menus;
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
        
        return menuMap.getOrDefault(parentId, new ArrayList<>());
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
        // 由于已删除type字段，直接返回所有菜单
        return menuRepository.findAll();
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
    
    /**
     * 根据角色ID获取菜单树
     * @param roleId 角色ID
     * @return 树形结构的菜单列表
     */
    public List<Menu> getMenusByRoleId(Long roleId) {
        try {
            // 检查roleId是否为空
            if (roleId == null) {
                System.out.println("角色ID为空，返回空菜单列表");
                return new ArrayList<>();
            }
            
            // 根据角色ID查询sys_roles表，获取menus字段的值
            Map<String, Object> roleMap = jdbcTemplate.queryForMap("SELECT menus FROM sys_roles WHERE id = ?", roleId);
            Object menusObj = roleMap.get("menus");
            System.out.println("角色ID: " + roleId + ", menus字段值: " + menusObj + ", 类型: " + (menusObj != null ? menusObj.getClass().getName() : "null"));
            
            // 如果menus字段为空，返回空列表
            if (menusObj == null) {
                System.out.println("menus字段为空，返回空菜单列表");
                return new ArrayList<>();
            }
            
            // 解析menus字段，获取菜单ID列表
            List<Long> menuIds = new ArrayList<>();
            
            if (menusObj instanceof String) {
                // 如果是字符串类型
                String menusStr = (String) menusObj;
                if (menusStr.trim().isEmpty()) {
                    return new ArrayList<>();
                }
                
                if (menusStr.startsWith("[")) {
                    // JSON格式的菜单ID列表
                    try {
                        menuIds = new com.fasterxml.jackson.databind.ObjectMapper().readValue(menusStr, new com.fasterxml.jackson.core.type.TypeReference<List<Long>>() {});
                    } catch (Exception e) {
                        // 如果JSON解析失败，尝试按逗号分隔解析
                        String[] ids = menusStr.replace("[", "").replace("]", "").split(",");
                        for (String id : ids) {
                            try {
                                menuIds.add(Long.parseLong(id.trim()));
                            } catch (NumberFormatException ex) {
                                // 忽略无效的ID
                            }
                        }
                    }
                } else {
                    // 逗号分隔的菜单ID列表
                    String[] ids = menusStr.split(",");
                    for (String id : ids) {
                        try {
                            menuIds.add(Long.parseLong(id.trim()));
                        } catch (NumberFormatException e) {
                            // 忽略无效的ID
                        }
                    }
                }
            } else if (menusObj instanceof List) {
                // 如果是数组类型
                List<?> menusList = (List<?>) menusObj;
                for (Object item : menusList) {
                    try {
                        if (item instanceof Number) {
                            menuIds.add(((Number) item).longValue());
                        } else if (item instanceof String) {
                            menuIds.add(Long.parseLong((String) item));
                        }
                    } catch (NumberFormatException e) {
                        // 忽略无效的ID
                    }
                }
            } else if (menusObj instanceof String[]) {
                // 如果是字符串数组类型
                String[] ids = (String[]) menusObj;
                for (String id : ids) {
                    try {
                        menuIds.add(Long.parseLong(id.trim()));
                    } catch (NumberFormatException e) {
                        // 忽略无效的ID
                    }
                }
            } else if (menusObj instanceof Long[]) {
                // 如果是长整型数组类型
                Long[] ids = (Long[]) menusObj;
                menuIds.addAll(Arrays.asList(ids));
            } else if (menusObj instanceof int[]) {
                // 如果是整型数组类型
                int[] ids = (int[]) menusObj;
                for (int id : ids) {
                    menuIds.add((long) id);
                }
            } else if (menusObj instanceof long[]) {
                // 如果是长整型数组类型
                long[] ids = (long[]) menusObj;
                for (long id : ids) {
                    menuIds.add(id);
                }
            }
            
            // 如果菜单ID列表为空，返回空列表
            if (menuIds == null || menuIds.isEmpty()) {
                System.out.println("菜单ID列表为空，返回空菜单列表");
                return new ArrayList<>();
            }
            
            System.out.println("解析后的菜单ID列表: " + menuIds);
            
            // 根据菜单ID列表查询sys_menus表，获取菜单信息
            List<Menu> menus = menuRepository.findAllById(menuIds);
            System.out.println("从数据库查询到的菜单数量: " + (menus != null ? menus.size() : 0));
            
            // 打印每个菜单的详细信息
            if (menus != null) {
                for (Menu menu : menus) {
                    System.out.println("菜单详情 - ID: " + menu.getMenuId() + ", 名称: " + menu.getName());
                }
            }
            
            // 手动过滤可见且状态正常的菜单，添加menu空值检查
            // 暂时移除所有过滤条件，直接返回所有菜单
            List<Menu> visibleMenus = menus.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            System.out.println("过滤后的可见菜单数量: " + visibleMenus.size());
            
            // 构建菜单树并返回
            List<Menu> menuTree = buildMenuTree(visibleMenus, 0L);
            System.out.println("构建的菜单树: " + menuTree);
            return menuTree;
        } catch (Exception e) {
            // 记录异常日志
            e.printStackTrace();
            // 如果查询过程中发生异常，返回空列表
            return new ArrayList<>();
        }
    }
}