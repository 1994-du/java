package com.springbootproject.Config;

import com.springbootproject.Entity.User;
import com.springbootproject.Repository.UserRepository;
import com.springbootproject.Service.MenuService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MenuService menuService;
    private final String initialAdminPassword;

    public DataInitializer(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            MenuService menuService,
            @Value("${app.initial-admin-password:}") String initialAdminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.menuService = menuService;
        this.initialAdminPassword = initialAdminPassword;
    }

    @Override
    public void run(String... args) throws Exception {
        // 初始化管理员用户
        User adminUser = userRepository.findByUsername("admin");
        if (adminUser == null && !initialAdminPassword.isBlank()) {
            adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPassword(passwordEncoder.encode(initialAdminPassword));
            adminUser.setRoleId(1L);
            adminUser.setRoleName("超级管理员");
            adminUser.setAvatar("/uploads/avatars/default.jpeg");
            adminUser.setGender("男");

            userRepository.save(adminUser);
        }

        // 预加载所有角色的菜单数据到内存缓存（消除登录时的DB查询）
        menuService.preloadAllRoleMenus();
    }
}