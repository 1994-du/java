package com.springbootproject.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.springbootproject.Entity.User;
import com.springbootproject.Repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service  // 标记为服务层组件
public class UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 通过ID查找用户
    public User findUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
    
    // 通过用户名查找用户
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    // 用于AuthController中的方法名
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    // 根据ID删除用户
    public void deleteUserById(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("用户不存在");
        }
        
        try {
            // 直接删除用户记录
            userRepository.deleteById(userId);
            System.out.println("用户删除成功，用户ID: " + userId);
        } catch (Exception e) {
            System.err.println("删除用户失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("删除用户失败: " + e.getMessage());
        }
    }

    // 注：createUser方法已被createUserWithAvatarAndRole方法替代

    // 登录验证方法
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            return user;
        }
        return null; // 登录失败返回null
    }
    
    // 获取所有用户
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    // 获取分页用户列表
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    // 更新用户头像
    public User updateUserAvatar(String username, String avatarUrl) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        user.setAvatar(avatarUrl);
        return userRepository.save(user);
    }
    
    // 更新用户信息
    public User updateUser(Long id, String username, String avatar, Long roleId, String gender) {
        // 根据ID查找用户
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 更新用户名（如果提供）
        if (username != null && !username.trim().isEmpty()) {
            // 检查新用户名是否已被其他用户使用
            User existingUser = userRepository.findByUsername(username);
            if (existingUser != null && !existingUser.getId().equals(id)) {
                throw new RuntimeException("用户名已存在");
            }
            user.setUsername(username);
        }
        
        // 更新头像（如果提供）
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        
        // 更新性别（如果提供）
        if (gender != null) {
            user.setGender(gender);
        }
        
        // 更新角色ID和角色名称（如果提供）
        if (roleId != null) {
            user.setRoleId(roleId);
            
            // 尝试查询角色名称
            try {
                Map<String, Object> roleMap = jdbcTemplate.queryForMap("SELECT id, name FROM sys_roles WHERE id = ?", roleId);
                if (roleMap != null) {
                    user.setRoleName((String) roleMap.get("name"));
                } else {
                    user.setRoleName("未知角色");
                }
            } catch (Exception e) {
                System.err.println("角色信息查询失败: " + e.getMessage());
                user.setRoleName("查询失败");
            }
        }
        
        // 保存更新后的用户
        return userRepository.save(user);
    }
    
    // 创建用户（包含头像和角色）
    public User createUserWithAvatarAndRole(String username, String password, String avatarUrl, Long roleId, String gender) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 创建用户对象
        User user = new User();
        user.setUsername(username);
        
        // 加密密码
        user.setPassword(passwordEncoder.encode("123456"));
        
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            user.setAvatar(avatarUrl);
        }
        
        // 设置性别
        if (gender != null) {
            user.setGender(gender);
        }
        
        // 角色相关信息
        Map<String, Object> roleMap = null;
        
        // 记录传入的roleId参数
        System.out.println("传入的roleId参数: " + roleId);
        
        // 直接设置用户角色ID，即使没有查询到角色信息
        if (roleId != null) {
            // 直接设置roleId到用户对象
            user.setRoleId(roleId);
            System.out.println("直接设置用户roleId: " + roleId);
            
            // 同时尝试查询角色名称
            try {
                System.out.println("开始查询角色信息，roleId: " + roleId);
                // 查询角色信息（id和name）
                roleMap = jdbcTemplate.queryForMap("SELECT id, name FROM sys_roles WHERE id = ?", roleId);
                if (roleMap != null) {
                    System.out.println("找到角色信息: " + roleMap);
                    // 设置角色名称
                    user.setRoleName((String) roleMap.get("name"));
                    System.out.println("已设置用户角色信息: roleId=" + user.getRoleId() + ", roleName=" + user.getRoleName());
                } else {
                    System.out.println("未找到角色信息，roleId: " + roleId);
                    // 即使没有找到角色信息，也要保持roleId
                    user.setRoleId(roleId);
                    user.setRoleName("未知角色");
                }
            } catch (Exception e) {
                // 角色查询失败记录日志但不影响用户创建
                System.err.println("角色信息查询失败: " + e.getMessage());
                e.printStackTrace();
                // 即使发生异常，也要保持roleId
                user.setRoleId(roleId);
                user.setRoleName("查询失败");
            }
        } else {
            System.out.println("roleId参数为null，不设置角色信息");
        }
        
        // 保存用户到数据库前，打印用户对象信息
        System.out.println("保存前的用户对象: id=" + user.getId() + ", roleId=" + user.getRoleId() + ", roleName=" + user.getRoleName() + ", gender=" + user.getGender());
        
        // 先保存用户到数据库，获取用户ID
        user = userRepository.save(user);
        
        // 保存后再次打印用户对象信息，确认角色信息是否正确保存
        System.out.println("保存后的用户对象: id=" + user.getId() + ", roleId=" + user.getRoleId() + ", roleName=" + user.getRoleName() + ", gender=" + user.getGender());
        
        // 尝试从数据库重新加载用户，验证角色信息是否持久化
        User reloadedUser = userRepository.findById(user.getId()).orElse(null);
        if (reloadedUser != null) {
            System.out.println("从数据库重新加载的用户: roleId=" + reloadedUser.getRoleId() + ", roleName=" + reloadedUser.getRoleName() + ", gender=" + reloadedUser.getGender());
        }
        
        // 用户保存成功后，如果有角色信息，插入用户角色关联记录
        if (roleId != null && roleMap != null) {
            try {
                int rowsAffected = jdbcTemplate.update("INSERT INTO user_role (user_id, role_id) VALUES (?, ?)", user.getId(), roleId);
                System.out.println("用户角色关联记录插入完成，影响行数: " + rowsAffected);
            } catch (Exception e) {
                // 用户角色关联失败不影响用户创建
                System.err.println("用户角色关联记录插入失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return user;
    }
    
    // 用户注册方法
    public User register(String username, String password, String gender) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 创建用户对象
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoleId(2L); // 默认普通用户角色ID
        user.setRoleName("普通用户"); // 默认角色名称
        user.setAvatar("/uploads/avatars/default.jpeg"); // 设置默认头像路径
        user.setGender(gender); // 设置性别
        
        // 保存用户到数据库
        return userRepository.save(user);
    }
    
    // 密码重置方法
    public User resetPassword(String username, String newPassword) {
        // 检查用户是否存在
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名不存在");
        }
        
        // 密码加密并更新
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }
    
    // 重置用户密码为默认密码"123456"（加密）
    public User resetPassword(Long userId) {
        // 根据ID查找用户
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 设置默认密码并加密
        user.setPassword(passwordEncoder.encode("123456"));
        
        // 保存更新后的用户
        return userRepository.save(user);
    }
}