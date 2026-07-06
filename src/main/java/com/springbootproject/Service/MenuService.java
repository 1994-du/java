package com.springbootproject.Service;

import com.springbootproject.Entity.Menu;
import com.springbootproject.Repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 直接内存缓存，避免重复解析角色菜单
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
        return buildMenuTree(getActiveMenus(), 0L);
    }

    public List<Menu> getVisibleMenuTree() {
        return buildMenuTree(getVisibleMenus(), 0L);
    }

    public List<Menu> getChildMenus(Long parentId) {
        final Long finalParentId = parentId == null ? 0L : parentId;
        return menuRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(menu -> Objects.equals(normalizeParentId(menu.getParentId()), finalParentId))
                .collect(Collectors.toList());
    }

    public List<Menu> buildMenuTree(List<Menu> menus, Long parentId) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }

        Long normalizedParentId = parentId == null ? 0L : parentId;
        List<Menu> result = new ArrayList<>();

        for (Menu menu : menus) {
            if (menu == null) {
                continue;
            }

            if (!Objects.equals(normalizeParentId(menu.getParentId()), normalizedParentId)) {
                continue;
            }

            List<Menu> children = buildMenuTree(menus, menu.getId());
            children.sort(Comparator.comparingInt(this::safeSort));
            menu.setChildren(children.isEmpty() ? null : children);
            result.add(menu);
        }

        result.sort(Comparator.comparingInt(this::safeSort));
        return result;
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
     * 根据角色ID获取菜单树。
     * 如果角色只勾选了子菜单，这里会自动补齐所有祖先节点，确保前端能正常展示父级菜单。
     */
    public List<Menu> getMenusByRoleId(Long roleId) {
        if (roleId == null) {
            return new ArrayList<>();
        }

        List<Menu> cached = menuCache.get(roleId);
        if (cached != null) {
            return cloneMenuTree(cached);
        }

        List<Long> selectedMenuIds = loadMenuIdsByRoleId(roleId);
        if (selectedMenuIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Menu> allMenusMap = menuRepository.findAll().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Menu::getId, this::cloneMenuNode, (left, right) -> left, LinkedHashMap::new));

        LinkedHashMap<Long, Menu> accessibleMenus = new LinkedHashMap<>();
        for (Long menuId : selectedMenuIds) {
            appendMenuWithAncestors(menuId, allMenusMap, accessibleMenus);
        }

        List<Menu> menuTree = buildMenuTree(new ArrayList<>(accessibleMenus.values()), 0L);
        menuCache.put(roleId, cloneMenuTree(menuTree));
        return cloneMenuTree(menuTree);
    }

    public void preloadAllRoleMenus() {
        try {
            List<Map<String, Object>> roles = jdbcTemplate.queryForList(
                    "SELECT id FROM sys_roles WHERE menus IS NOT NULL AND menus <> ''");

            for (Map<String, Object> role : roles) {
                Number roleIdNum = (Number) role.get("id");
                if (roleIdNum != null) {
                    getMenusByRoleId(roleIdNum.longValue());
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void clearMenuCache() {
        menuCache.clear();
    }

    private List<Long> loadMenuIdsByRoleId(Long roleId) {
        try {
            Map<String, Object> roleMap = jdbcTemplate.queryForMap(
                    "SELECT menus FROM sys_roles WHERE id = ?", roleId);
            return parseMenuIds(roleMap.get("menus"));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void appendMenuWithAncestors(Long menuId, Map<Long, Menu> allMenusMap, LinkedHashMap<Long, Menu> result) {
        Menu current = allMenusMap.get(menuId);
        while (current != null) {
            result.putIfAbsent(current.getId(), cloneMenuNode(current));

            Long parentId = current.getParentId();
            if (parentId == null || parentId == 0L) {
                break;
            }
            current = allMenusMap.get(parentId);
        }
    }

    private Menu cloneMenuNode(Menu source) {
        Menu target = new Menu();
        target.setId(source.getId());
        target.setPath(source.getPath());
        target.setName(source.getName());
        target.setComponent(source.getComponent());
        target.setMeta(source.getMeta());
        target.setIcon(source.getIcon());
        target.setParentId(source.getParentId());
        target.setSort(source.getSort());
        target.setChildren(null);
        return target;
    }

    private List<Menu> cloneMenuTree(List<Menu> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }

        List<Menu> copied = new ArrayList<>();
        for (Menu menu : menus) {
            if (menu == null) {
                continue;
            }
            Menu cloned = cloneMenuNode(menu);
            cloned.setChildren(cloneMenuTree(menu.getChildren()));
            copied.add(cloned);
        }
        return copied;
    }

    private int safeSort(Menu menu) {
        return menu != null && menu.getSort() != null ? menu.getSort() : 0;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private List<Long> parseMenuIds(Object menusObj) {
        List<Long> menuIds = new ArrayList<>();

        if (menusObj == null) {
            return menuIds;
        }

        if (menusObj instanceof String menusStr) {
            String trimmed = menusStr.trim();
            if (trimmed.isEmpty()) {
                return menuIds;
            }

            if (trimmed.startsWith("[")) {
                try {
                    return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                            trimmed,
                            new com.fasterxml.jackson.core.type.TypeReference<List<Long>>() {
                            });
                } catch (Exception ignored) {
                }
            }

            String[] ids = trimmed.replace("[", "").replace("]", "").split(",");
            for (String id : ids) {
                try {
                    menuIds.add(Long.parseLong(id.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
            return menuIds;
        }

        if (menusObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Number number) {
                    menuIds.add(number.longValue());
                } else if (item instanceof String str) {
                    try {
                        menuIds.add(Long.parseLong(str.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        return menuIds;
    }
}
