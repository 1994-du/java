package com.springbootproject.Config;

import com.springbootproject.Entity.Menu;
import com.springbootproject.Entity.User;
import com.springbootproject.Repository.MenuRepository;
import com.springbootproject.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 数据初始化类
 * 在应用启动时自动初始化用户路由测试数据
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private MenuRepository menuRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // 不再自动初始化菜单和角色数据，只从MySQL数据库中查询
        
        // 只初始化管理员用户（如果不存在）
        initializeUsers();
    }
    
    /**
     * 初始化测试用户
     */
    private void initializeUsers() {
        // 强制重新创建管理员用户，确保密码字段正确
        User adminUser = userRepository.findByUsername("admin");
        if (adminUser == null) {
            adminUser = new User();
        }
        
        adminUser.setUsername("admin");
        String encodedPassword = passwordEncoder.encode("admin");
        adminUser.setPassword(encodedPassword);
        adminUser.setAvatar("/api/user/avatar/default");
        adminUser.setRoleId(1L);
        adminUser.setRoleName("管理员");
        
        userRepository.save(adminUser);
        System.out.println("初始化管理员用户完成: 用户名=admin, 密码=admin");
        System.out.println("加密后的密码: " + encodedPassword);
        System.out.println("密码长度: " + encodedPassword.length());
        System.out.println("验证测试: " + passwordEncoder.matches("admin", encodedPassword));
    }

    /**
     * 初始化菜单测试数据
     */
    private void initializeMenus() {
        // 先创建顶级菜单
        Menu dashboardMenu = createMenu("仪表盘", 1, "/dashboard", "dashboard/index", 1, 1, 1, "el-icon-data-line", 
                "dashboard:view", "{\"title\":\"仪表盘\",\"keepAlive\":false}", null);
        Menu userManagementMenu = createMenu("用户管理", 1, "/system", "Layout", 2, 1, 1, "el-icon-user", 
                "system:user:manage", "{\"title\":\"用户管理\",\"keepAlive\":false}", null);
        Menu systemSettingMenu = createMenu("系统设置", 1, "/setting", "Layout", 3, 1, 1, "el-icon-setting", 
                "system:setting:manage", "{\"title\":\"系统设置\",\"keepAlive\":false}", null);
        
        // 保存顶级菜单
        dashboardMenu = menuRepository.save(dashboardMenu);
        userManagementMenu = menuRepository.save(userManagementMenu);
        systemSettingMenu = menuRepository.save(systemSettingMenu);
        
        // 创建二级菜单
        Menu userListMenu = createMenu("用户列表", 2, "user", "system/user/index", 1, 1, 1, "el-icon-s-custom", 
                "system:user:list", "{\"title\":\"用户列表\",\"keepAlive\":true}", userManagementMenu.getMenuId());
        Menu roleListMenu = createMenu("角色管理", 2, "role", "system/role/index", 2, 1, 1, "el-icon-roles", 
                "system:role:list", "{\"title\":\"角色管理\",\"keepAlive\":true}", userManagementMenu.getMenuId());
        Menu profileSettingMenu = createMenu("个人设置", 2, "profile", "setting/profile/index", 1, 1, 1, "el-icon-user-solid", 
                "system:setting:profile", "{\"title\":\"个人设置\",\"keepAlive\":true}", systemSettingMenu.getMenuId());
        
        // 保存二级菜单
        userListMenu = menuRepository.save(userListMenu);
        roleListMenu = menuRepository.save(roleListMenu);
        menuRepository.save(profileSettingMenu);
        
        // 创建三级菜单（按钮）
        Menu userAddButton = createMenu("添加用户", 3, "", "", 1, 1, 1, "", 
                "system:user:add", "{\"title\":\"添加用户\"}", userListMenu.getMenuId());
        Menu userEditButton = createMenu("编辑用户", 3, "", "", 2, 1, 1, "", 
                "system:user:edit", "{\"title\":\"编辑用户\"}", userListMenu.getMenuId());
        
        // 保存三级菜单
        menuRepository.save(userAddButton);
        menuRepository.save(userEditButton);
        
        System.out.println("菜单测试数据初始化完成！");
    }
    
    /**
     * 创建菜单的工具方法
     */
    private Menu createMenu(String name, int type, String path, String componentPath, int sort, 
                           int visible, int status, String icon, String perms, String meta, Long parentId) {
        Menu menu = new Menu();
        menu.setName(name);
        menu.setType(type);
        menu.setPath(path);
        menu.setComponentPath(componentPath);
        menu.setSort(sort);
        menu.setVisible(visible);
        menu.setStatus(status);
        menu.setIcon(icon);
        menu.setPerms(perms);
        menu.setMeta(meta);
        menu.setParentId(parentId != null ? parentId : 0L);
        menu.initTimestamps();
        return menu;
    }
    
    /**
     * 初始化角色数据
     */
    private void initializeRoles() {
        try {
            // 检查sys_roles表是否存在，如果不存在则创建
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_roles (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) NOT NULL, menus VARCHAR(1000) DEFAULT '')");
            
            // 查询sys_roles表中的数据数量
            int roleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_roles", Integer.class);
            
            // 如果sys_roles表中没有数据，则初始化数据
            if (roleCount == 0) {
                // 获取所有菜单ID
                List<Menu> allMenus = menuRepository.findAll();
                List<Long> menuIds = new ArrayList<>();
                for (Menu menu : allMenus) {
                    menuIds.add(menu.getMenuId());
                }
                
                // 将菜单ID转换为JSON数组字符串
                String menusStr = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(menuIds);
                
                // 插入管理员角色
                jdbcTemplate.update("INSERT INTO sys_roles (name, menus) VALUES (?, ?)", "管理员", menusStr);
                
                // 插入普通用户角色（只包含部分菜单）
                List<Long> normalUserMenuIds = menuIds.subList(0, Math.min(menuIds.size(), 6));
                String normalUserMenusStr = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(normalUserMenuIds);
                jdbcTemplate.update("INSERT INTO sys_roles (name, menus) VALUES (?, ?)", "普通用户", normalUserMenusStr);
                
                System.out.println("角色数据初始化完成！");
                System.out.println("管理员角色菜单: " + menusStr);
                System.out.println("普通用户角色菜单: " + normalUserMenusStr);
            }
        } catch (Exception e) {
            System.err.println("初始化角色数据时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}