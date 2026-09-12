package com.springbootproject.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springbootproject.Entity.Role;
import com.springbootproject.Repository.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final MenuService menuService;
    private final ObjectMapper objectMapper;

    public RoleService(RoleRepository roleRepository, MenuService menuService, ObjectMapper objectMapper) {
        this.roleRepository = roleRepository;
        this.menuService = menuService;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> getRoleDictionary() {
        return roleRepository.findAll().stream().map(this::toMap).toList();
    }

    public Map<String, Object> getRoles(int page, int size, String keyword) {
        if (page < 1 || size < 1) {
            throw new IllegalArgumentException("页码和分页大小必须大于0");
        }

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Role> rolePage = keyword == null || keyword.isBlank()
                ? roleRepository.findAll(pageable)
                : roleRepository.findByNameContaining(keyword.trim(), pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("total", rolePage.getTotalElements());
        result.put("totalPages", rolePage.getTotalPages());
        result.put("list", rolePage.getContent().stream().map(this::toMap).toList());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Transactional
    public void updateRole(Map<String, Object> roleData) {
        Long id = requiredId(roleData);
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在"));

        boolean updated = false;
        if (roleData.containsKey("name") && roleData.get("name") != null) {
            role.setName(roleData.get("name").toString());
            updated = true;
        }
        if (roleData.containsKey("roleDesc") && roleData.get("roleDesc") != null) {
            role.setRoleDesc(roleData.get("roleDesc").toString());
            updated = true;
        }
        if (roleData.containsKey("menus") && roleData.get("menus") != null) {
            role.setMenus(toMenusJson(roleData.get("menus")));
            updated = true;
        }
        if (!updated) {
            throw new IllegalArgumentException("没有需要更新的字段");
        }

        roleRepository.save(Objects.requireNonNull(role));
        menuService.clearMenuCache();
    }

    @Transactional
    public void addRole(Map<String, Object> roleData) {
        if (roleData == null) {
            throw new IllegalArgumentException("角色数据不能为空");
        }
        String name = roleData.get("name") == null
                ? ""
                : roleData.get("name").toString().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("角色名称不能为空");
        }

        Role role = new Role();
        role.setName(name);
        role.setRoleDesc(roleData.get("roleDesc") == null ? null : roleData.get("roleDesc").toString());
        role.setMenus(roleData.get("menus") == null ? "[]" : toMenusJson(roleData.get("menus")));
        roleRepository.save(role);
        menuService.clearMenuCache();
    }

    @Transactional
    public void deleteRole(Map<String, Object> roleData) {
        Long id = requiredId(roleData);
        if (!roleRepository.existsById(id)) {
            throw new IllegalArgumentException("角色不存在");
        }
        roleRepository.deleteById(id);
        menuService.clearMenuCache();
    }

    private Map<String, Object> toMap(Role role) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", role.getId());
        result.put("name", role.getName());
        result.put("roleDesc", role.getRoleDesc());
        result.put("menus", parseMenus(role.getMenus()));
        return result;
    }

    private List<Integer> parseMenus(String menus) {
        if (menus == null || menus.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(menus, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return new ArrayList<>();
        }
    }

    private String toMenusJson(Object menus) {
        try {
            List<Integer> menuIds = objectMapper.convertValue(menus, new TypeReference<>() {
            });
            return objectMapper.writeValueAsString(menuIds);
        } catch (IllegalArgumentException | JsonProcessingException exception) {
            throw new IllegalArgumentException("menus字段格式错误", exception);
        }
    }

    @NonNull
    private Long requiredId(Map<String, Object> roleData) {
        if (roleData == null || roleData.get("id") == null) {
            throw new IllegalArgumentException("角色ID不能为空");
        }
        try {
            return Objects.requireNonNull(Long.valueOf(roleData.get("id").toString()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("角色ID格式错误", exception);
        }
    }
}