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

@Service
public class UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public User findUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
    
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public void deleteUserById(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("用户不存在");
        }
        
        try {
            userRepository.deleteById(userId);
        } catch (Exception e) {
            throw new RuntimeException("删除用户失败: " + e.getMessage());
        }
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            return user;
        }
        return null;
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    public List<User> searchByUsername(String username) {
        return userRepository.findByUsernameContaining(username);
    }
    
    public Page<User> searchUsers(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findByUsernameContaining(keyword, pageable);
    }
    
    public User updateUserAvatar(String username, String avatarUrl) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        user.setAvatar(avatarUrl);
        return userRepository.save(user);
    }
    
    public User updateUser(Long id, String username, String avatar, Long roleId, String gender) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        if (username != null && !username.trim().isEmpty()) {
            User existingUser = userRepository.findByUsername(username);
            if (existingUser != null && !existingUser.getId().equals(id)) {
                throw new RuntimeException("用户名已存在");
            }
            user.setUsername(username);
        }
        
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        
        if (gender != null) {
            user.setGender(gender);
        }
        
        if (roleId != null) {
            user.setRoleId(roleId);
            
            try {
                Map<String, Object> roleMap = jdbcTemplate.queryForMap("SELECT id, name FROM sys_roles WHERE id = ?", roleId);
                if (roleMap != null) {
                    user.setRoleName((String) roleMap.get("name"));
                } else {
                    user.setRoleName("未知角色");
                }
            } catch (Exception e) {
                user.setRoleName("查询失败");
            }
        }
        
        return userRepository.save(user);
    }
    
    public User createUserWithAvatarAndRole(String username, String password, String avatarUrl, Long roleId, String gender) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("123456"));
        
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            user.setAvatar(avatarUrl);
        }
        
        if (gender != null) {
            user.setGender(gender);
        }
        
        if (roleId != null) {
            user.setRoleId(roleId);
            
            try {
                Map<String, Object> roleMap = jdbcTemplate.queryForMap("SELECT id, name FROM sys_roles WHERE id = ?", roleId);
                if (roleMap != null) {
                    user.setRoleName((String) roleMap.get("name"));
                } else {
                    user.setRoleId(roleId);
                    user.setRoleName("未知角色");
                }
            } catch (Exception e) {
                user.setRoleId(roleId);
                user.setRoleName("查询失败");
            }
        }
        
        user = userRepository.save(user);
        
        // 插入用户角色关联记录
        if (roleId != null) {
            try {
                jdbcTemplate.update("INSERT INTO user_role (user_id, role_id) VALUES (?, ?)", user.getId(), roleId);
            } catch (Exception e) {
                // 用户角色关联失败不影响用户创建
            }
        }
        
        return user;
    }
    
    public User register(String username, String password, String gender) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoleId(2L);
        user.setRoleName("普通用户");
        user.setAvatar("/uploads/avatars/default.jpeg");
        user.setGender(gender);
        
        return userRepository.save(user);
    }
    
    public User resetPassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名不存在");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    public User resetPassword(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        user.setPassword(passwordEncoder.encode("123456"));
        return userRepository.save(user);
    }
}