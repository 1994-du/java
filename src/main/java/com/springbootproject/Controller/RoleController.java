package com.springbootproject.Controller;

import com.springbootproject.Model.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RoleController {

    @Autowired
    private DataSource dataSource;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取所有角色字典
     * 从sys_roles表中获取所有角色数据
     */
    @GetMapping("/getRoleDict")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRoleDict() {
        List<Map<String, Object>> roleList = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT * FROM sys_roles")) {

            ResultSet rs = stmt.executeQuery();

            int columnCount = rs.getMetaData().getColumnCount();

            while (rs.next()) {
                Map<String, Object> roleMap = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rs.getMetaData().getColumnName(i);
                    Object value = rs.getObject(i);
                    
                    if ("menus".equals(columnName) && value != null) {
                        try {
                            List<Integer> menusList = objectMapper.readValue(value.toString(), new TypeReference<List<Integer>>() {});
                            roleMap.put(columnName, menusList);
                        } catch (Exception e) {
                            System.out.println("解析menus字段失败: " + value);
                            roleMap.put(columnName, new ArrayList<>());
                        }
                    } else {
                        roleMap.put(columnName, value);
                    }
                }
                roleList.add(roleMap);
            }

            return ResponseEntity.ok(ApiResponse.success("获取角色字典成功", roleList));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取角色字典失败: " + e.getMessage()));
        }
    }

    /**
     * 分页查询角色列表
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 分页角色数据
     */
    @PostMapping("/getRoles")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> roleList = new ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            String countSql = "SELECT COUNT(*) FROM sys_roles";
            try (PreparedStatement countStmt = connection.prepareStatement(countSql)) {
                ResultSet countRs = countStmt.executeQuery();
                if (countRs.next()) {
                    long total = countRs.getLong(1);
                    result.put("total", total);
                    result.put("totalPages", (int) Math.ceil((double) total / size));
                }
            }

            int offset = (page - 1) * size;
            String sql = "SELECT * FROM sys_roles LIMIT ? OFFSET ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, size);
                stmt.setInt(2, offset);

                ResultSet rs = stmt.executeQuery();
                int columnCount = rs.getMetaData().getColumnCount();

                while (rs.next()) {
                    Map<String, Object> roleMap = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rs.getMetaData().getColumnName(i);
                        Object value = rs.getObject(i);
                        
                        if ("menus".equals(columnName) && value != null) {
                            try {
                                List<Integer> menusList = objectMapper.readValue(value.toString(), new TypeReference<List<Integer>>() {});
                                roleMap.put(columnName, menusList);
                            } catch (Exception e) {
                                System.out.println("解析menus字段失败: " + value);
                                roleMap.put(columnName, new ArrayList<>());
                            }
                        } else {
                            roleMap.put(columnName, value);
                        }
                    }
                    roleList.add(roleMap);
                }
            }

            result.put("list", roleList);
            result.put("page", page);
            result.put("size", size);

            return ResponseEntity.ok(ApiResponse.success("获取角色列表成功", result));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取角色列表失败: " + e.getMessage()));
        }
    }

    /**
     * 更新角色信息
     * @param roleData 角色数据，包含id和其他需要更新的字段
     * @return 更新结果
     */
    @PostMapping("/setRole")
    public ResponseEntity<ApiResponse<Object>> setRole(@RequestBody Map<String, Object> roleData) {
        try {
            if (!roleData.containsKey("id") || roleData.get("id") == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("角色ID不能为空"));
            }

            Long id = Long.valueOf(roleData.get("id").toString());

            StringBuilder sql = new StringBuilder("UPDATE sys_roles SET ");
            List<Object> params = new ArrayList<>();
            List<String> updates = new ArrayList<>();

            if (roleData.containsKey("name") && roleData.get("name") != null) {
                updates.add("name = ?");
                params.add(roleData.get("name"));
            }
            if (roleData.containsKey("roleDesc") && roleData.get("roleDesc") != null) {
                updates.add("roleDesc = ?");
                params.add(roleData.get("roleDesc"));
            }
            if (roleData.containsKey("menus") && roleData.get("menus") != null) {
                try {
                    List<Integer> menusList = (List<Integer>) roleData.get("menus");
                    String menusJson = objectMapper.writeValueAsString(menusList);
                    updates.add("menus = ?");
                    params.add(menusJson);
                } catch (Exception e) {
                    System.out.println("转换menus字段失败: " + roleData.get("menus"));
                    return ResponseEntity.badRequest().body(ApiResponse.error("menus字段格式错误"));
                }
            }

            if (updates.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("没有需要更新的字段"));
            }

            sql.append(String.join(", ", updates));
            sql.append(" WHERE id = ?");
            params.add(id);

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql.toString())) {

                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    return ResponseEntity.ok(ApiResponse.success("更新角色成功", null));
                } else {
                    return ResponseEntity.badRequest().body(ApiResponse.error("角色不存在"));
                }

            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(500).body(ApiResponse.error("更新角色失败: " + e.getMessage()));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("更新角色失败: " + e.getMessage()));
        }
    }

    /**
     * 添加角色
     * @param roleData 角色数据，包含角色名称等字段
     * @return 添加结果
     */
    @PostMapping("/addRole")
    public ResponseEntity<ApiResponse<Object>> addRole(@RequestBody Map<String, Object> roleData) {
        try {
            if (!roleData.containsKey("name") || roleData.get("name") == null || roleData.get("name").toString().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("角色名称不能为空"));
            }

            String name = roleData.get("name").toString();
            String roleDesc = roleData.containsKey("roleDesc") && roleData.get("roleDesc") != null ? roleData.get("roleDesc").toString() : null;
            String menusJson = "[]";

            if (roleData.containsKey("menus") && roleData.get("menus") != null) {
                try {
                    List<Integer> menusList = (List<Integer>) roleData.get("menus");
                    menusJson = objectMapper.writeValueAsString(menusList);
                } catch (Exception e) {
                    System.out.println("转换menus字段失败: " + roleData.get("menus"));
                    return ResponseEntity.badRequest().body(ApiResponse.error("menus字段格式错误"));
                }
            }

            String sql = "INSERT INTO sys_roles (name, roleDesc, menus) VALUES (?, ?, ?)";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {

                stmt.setString(1, name);
                stmt.setString(2, roleDesc);
                stmt.setString(3, menusJson);

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    return ResponseEntity.ok(ApiResponse.success("添加角色成功", null));
                } else {
                    return ResponseEntity.status(500).body(ApiResponse.error("添加角色失败"));
                }

            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(500).body(ApiResponse.error("添加角色失败: " + e.getMessage()));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("添加角色失败: " + e.getMessage()));
        }
    }

    /**
     * 删除角色
     * @param roleData 角色数据，包含角色ID
     * @return 删除结果
     */
    @PostMapping("/delRole")
    public ResponseEntity<ApiResponse<Object>> delRole(@RequestBody Map<String, Object> roleData) {
        try {
            if (!roleData.containsKey("id") || roleData.get("id") == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("角色ID不能为空"));
            }

            Long id = Long.valueOf(roleData.get("id").toString());

            String sql = "DELETE FROM sys_roles WHERE id = ?";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {

                stmt.setLong(1, id);

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    return ResponseEntity.ok(ApiResponse.success("删除角色成功", null));
                } else {
                    return ResponseEntity.badRequest().body(ApiResponse.error("角色不存在"));
                }

            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(500).body(ApiResponse.error("删除角色失败: " + e.getMessage()));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("删除角色失败: " + e.getMessage()));
        }
    }
}