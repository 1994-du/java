package com.springbootproject.Service;

import com.springbootproject.Entity.Menu;
import com.springbootproject.Repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 直接内存缓存，不受Spring AOP代理限制
    private final Map<Long, List<Menu>> menuCache = new ConcurrentHashMap<>();

    public List<Menu> getAllMenus() {
        return menuRepository.findAll();
    }

    public List<Menu> getActiveMenus() {
        return menuRepository.findAll();
    }

    public List<Menu> getVisibleMenus() {
        return menuRepository.findAll();
    }

    public Optional<Menu> getMenuById(Long menuId) {
        if (menuId == null) {
            return Optional.empty();
        }
        return menuRepository.findById(menuId);
    }

    public Menu saveMenu(Menu menu) {
        if (menu == null) {
            throw new IllegalArgumentException("Menu cannot be null");
        }
        clearMenuCache();
        return menuRepository.save(menu);
    }

    public List<Menu> saveMenus(List<Menu> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        List<Menu> validMenus = menus.stream().filter(Objects::nonNull).collect(Collectors.toList());
        clearMenuCache();
        return menuRepository.saveAll(validMenus);
    }

    public void deleteMenu(Long menuId) {
        if (menuId == null) {
            throw new IllegalArgumentException("Menu ID cannot be null");
        }
        clearMenuCache();
        menuRepository.deleteById(menuId);
    }

    public List<Menu> getMenuTree() {
        List<Menu> allMenus = getActiveMenus();
        if (allMenus == null) {
            allMenus = new ArrayList<>();
        }
        return buildMenuTree(allMenus, 0L);
    }

    public List<Menu> getVisibleMenuTree() {
        List<Menu> menus = menuRepository.findAll();
        List<Menu> visibleMenus = menus.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return buildMenuTree(visibleMenus, 0L);
    }

    public List<Menu> getChildMenus(Long parentId) {
        final Long finalParentId = (parentId == null) ? 0L : parentId;
        return menuRepository.findAll().stream()
                .filter(menu -> menu.getParentId() != null && menu.getParentId().equals(finalParentId))
                .collect(Collectors.toList());
    }

    public List<Menu> buildMenuTree(List<Menu> menus, Long parentId) {
        if (menus == null) {
            return new ArrayList<>();
        }
        
        List<Menu> result = new ArrayList<>();
        
        for (Menu menu : menus) {
            Long menuParentId = menu.getParentId();
            boolean isMatch = false;
            
            if (parentId == 0L && menuParentId == null) {
                isMatch = true;
            } else if (parentId != 0L && menuParentId != null && menuParentId.equals(parentId)) {
                isMatch = true;
            }
            
            if (isMatch) {
                List<Menu> children = buildMenuTree(menus, menu.getId());
                children.sort(Comparator.comparingInt(m -> m.getSort() != null ? m.getSort() : 0));
                menu.setChildren(children.isEmpty() ? null : children);
                result.add(menu);
            }
        }
        
        result.sort(Comparator.comparingInt(m -> m.getSort() != null ? m.getSort() : 0));
        return result;
    }

    private List<Menu> buildTreeRecursive(Map<Long, List<Menu>> menuMap, Long parentId) {
        if (menuMap == null) {
            return new ArrayList<>();
        }
        return menuMap.getOrDefault(parentId, new ArrayList<>());
    }

    public List<Menu> getMenusByType(Integer type) {
        if (type == null) {
            return new ArrayList<>();
        }
        return menuRepository.findAll();
    }

    public List<Menu> searchMenus(String keyword) {
        if (keyword == null) {
            return new ArrayList<>();
        }
        return menuRepository.findByNameContaining(keyword);
    }

    /**
     * 根据角色ID获取菜单树（带内存缓存）
     * 首次访问后缓存到内存，后续请求0次DB查询
     */
    public List<Menu> getMenusByRoleId(Long roleId) {
        if (roleId == null) {
            return new ArrayList<>();
        }

        // 命中缓存直接返回
        List<Menu> cached = menuCache.get(roleId);
        if (cached != null) {
            return cached;
        }

        try {
            // 单条SQL：JOIN获取角色菜单
            String sql = "SELECT m.id, m.name, m.path, m.component, m.icon, " +
                    "m.parent_id AS parentId, m.sort, m.visible, m.status, m.permission " +
                    "FROM sys_menus m " +
                    "INNER JOIN sys_roles r ON JSON_CONTAINS(r.menus, CAST(m.id AS JSON)) " +
                    "WHERE r.id = ? AND m.status = 1 " +
                    "ORDER BY m.sort ASC";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, roleId);

            List<Menu> menus;
            if (!rows.isEmpty()) {
                menus = rowsToMenus(rows);
            } else {
                // JOIN无结果，降级为两次查询
                menus = loadMenusByRoleIdFallback(roleId);
            }

            List<Menu> tree = buildMenuTree(menus, 0L);
            menuCache.put(roleId, tree);
            return tree;

        } catch (Exception e) {
            List<Menu> fallback = loadMenusByRoleIdFallback(roleId);
            List<Menu> tree = buildMenuTree(fallback, 0L);
            menuCache.put(roleId, tree);
            return tree;
        }
    }

    /**
     * 降级方案：两次查询
     */
    private List<Menu> loadMenusByRoleIdFallback(Long roleId) {
        try {
            Map<String, Object> roleMap = jdbcTemplate.queryForMap(
                    "SELECT menus FROM sys_roles WHERE id = ?", roleId);

            Object menusObj = roleMap.get("menus");
            if (menusObj == null) {
                return new ArrayList<>();
            }

            List<Long> menuIds = parseMenuIds(menusObj);
            if (menuIds.isEmpty()) {
                return new ArrayList<>();
            }

            List<Menu> menus = menuRepository.findAllById(menuIds);
            if (menus == null || menus.isEmpty()) {
                return new ArrayList<>();
            }

            return menus.stream().filter(Objects::nonNull).collect(Collectors.toList());

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 启动时预加载所有角色的菜单数据到缓存
     */
    public void preloadAllRoleMenus() {
        try {
            List<Map<String, Object>> roles = jdbcTemplate.queryForList(
                    "SELECT id, menus FROM sys_roles WHERE menus IS NOT NULL");
            
            for (Map<String, Object> role : roles) {
                Number roleIdNum = (Number) role.get("id");
                if (roleIdNum == null) continue;
                Long roleId = roleIdNum.longValue();

                // 如果已有缓存则跳过
                if (menuCache.containsKey(roleId)) continue;

                try {
                    Object menusObj = role.get("menus");
                    if (menusObj == null) continue;

                    List<Long> menuIds = parseMenuIds(menusObj);
                    if (menuIds.isEmpty()) continue;

                    List<Menu> menus = menuRepository.findAllById(menuIds);
                    if (menus == null || menus.isEmpty()) continue;

                    List<Menu> tree = buildMenuTree(
                            menus.stream().filter(Objects::nonNull).collect(Collectors.toList()), 0L);
                    menuCache.put(roleId, tree);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    /**
     * 清除所有菜单缓存
     */
    public void clearMenuCache() {
        menuCache.clear();
    }

    // ---- 工具方法 ----

    private List<Menu> rowsToMenus(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> {
            Menu menu = new Menu();
            Object idObj = row.get("id");
            if (idObj instanceof Number) menu.setId(((Number) idObj).longValue());
            menu.setName((String) row.get("name"));
            menu.setPath((String) row.get("path"));
            menu.setComponent((String) row.get("component"));
            menu.setIcon((String) row.get("icon"));

            Object pidObj = row.get("parentId");
            if (pidObj instanceof Number) menu.setParentId(((Number) pidObj).longValue());

            Object sortObj = row.get("sort");
            if (sortObj instanceof Number) menu.setSort(((Number) sortObj).intValue());

            Object visObj = row.get("visible");
            if (visObj instanceof Number) menu.setVisible(((Number) visObj).intValue());

            Object stObj = row.get("status");
            if (stObj instanceof Number) menu.setStatus(((Number) stObj).intValue());

            menu.setPermission((String) row.get("permission"));
            return menu;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<Long> parseMenuIds(Object menusObj) {
        List<Long> menuIds = new ArrayList<>();

        if (menusObj instanceof String) {
            String menusStr = ((String) menusObj).trim();
            if (menusStr.isEmpty()) return menuIds;
            
            if (menusStr.startsWith("[")) {
                try {
                    menuIds = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                            menusStr, new com.fasterxml.jackson.core.type.TypeReference<List<Long>>() {});
                } catch (Exception e) {
                    String[] ids = menusStr.replace("[", "").replace("]", "").split(",");
                    for (String id : ids) {
                        try { menuIds.add(Long.parseLong(id.trim())); } catch (NumberFormatException ex) {}
                    }
                }
            } else {
                String[] ids = menusStr.split(",");
                for (String id : ids) {
                    try { menuIds.add(Long.parseLong(id.trim())); } catch (NumberFormatException e) {}
                }
            }
        } else if (menusObj instanceof List) {
            for (Object item : (List<?>) menusObj) {
                if (item instanceof Number) menuIds.add(((Number) item).longValue());
                else if (item instanceof String) { try { menuIds.add(Long.parseLong((String) item)); } catch (Exception e) {} }
            }
        } else if (menusObj instanceof Long[]) {
            menuIds.addAll(Arrays.asList((Long[]) menusObj));
        } else if (menusObj instanceof int[]) {
            for (int id : (int[]) menusObj) menuIds.add((long) id);
        } else if (menusObj instanceof long[]) {
            for (long id : (long[]) menusObj) menuIds.add(id);
        }

        return menuIds;
    }
}