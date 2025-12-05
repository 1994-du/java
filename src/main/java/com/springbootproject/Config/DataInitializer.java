package com.springbootproject.Config;

import com.springbootproject.Entity.Menu;
import com.springbootproject.Repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 数据初始化类
 * 在应用启动时自动初始化用户路由测试数据
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private MenuRepository menuRepository;

    @Override
    public void run(String... args) throws Exception {
        // 检查是否已经有菜单数据，如果没有则初始化测试数据
        if (menuRepository.count() == 0) {
            initializeMenus();
        }
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
}