package com.springbootproject.Config;

import com.springbootproject.Entity.User;
import com.springbootproject.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化类
 * 在应用启动时自动初始化用户路由测试数据
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

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
        // 检查管理员用户是否存在
        User adminUser = userRepository.findByUsername("admin");
        if (adminUser == null) {
            // 只在用户不存在时创建，设置默认密码
            adminUser = new User();
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
        } else {
            // 用户已存在，保持原有密码，只更新其他必要字段
            adminUser.setAvatar("/api/user/avatar/default");
            adminUser.setRoleId(1L);
            adminUser.setRoleName("管理员");
            
            userRepository.save(adminUser);
            System.out.println("管理员用户已存在，保持原有密码，只更新其他字段");
        }
    }


    
}