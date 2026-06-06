package com.springbootproject.Config;

import com.springbootproject.Entity.User;
import com.springbootproject.Repository.UserRepository;
import com.springbootproject.Service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
        // 检查是否已有管理员用户
        User adminUser = userRepository.findByUsername("admin");
        if (adminUser == null) {
            // 创建默认管理员用户
            adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPassword(passwordEncoder.encode("admin"));
            adminUser.setRoleId(1L);
            adminUser.setRoleName("超级管理员");
            adminUser.setAvatar("/uploads/avatars/default.jpeg");
            adminUser.setGender("男");
            
            userRepository.save(adminUser);
            System.out.println("初始化管理员用户完成: 用户名=admin, 密码=admin");
        } else {
            // 确保admin用户的密码是"admin"
            adminUser.setPassword(passwordEncoder.encode("admin"));
            userRepository.save(adminUser);
            System.out.println("管理员用户已存在，已重置密码为: admin");
        }

        // 打印所有用户信息（方便调试）
        List<User> allUsers = userRepository.findAll();
        System.out.println("当前数据库中的用户:");
        for (User user : allUsers) {
            System.out.println("  - 用户名: " + user.getUsername() + ", 角色: " + user.getRoleName());
        }
    }
}
