package com.springbootproject.Config;

import com.springbootproject.Entity.User;
import com.springbootproject.Repository.UserRepository;
import com.springbootproject.Service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MenuService menuService;

    @Override
    public void run(String... args) throws Exception {
        // 初始化管理员用户
        User adminUser = userRepository.findByUsername("admin");
        if (adminUser == null) {
            adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPassword(passwordEncoder.encode("admin"));
            adminUser.setRoleId(1L);
            adminUser.setRoleName("超级管理员");
            adminUser.setAvatar("/uploads/avatars/default.jpeg");
            adminUser.setGender("男");
            
            userRepository.save(adminUser);
        } else {
            adminUser.setPassword(passwordEncoder.encode("admin"));
            userRepository.save(adminUser);
        }

        // 预加载所有角色的菜单数据到内存缓存（消除登录时的DB查询）
        menuService.preloadAllRoleMenus();
    }
}