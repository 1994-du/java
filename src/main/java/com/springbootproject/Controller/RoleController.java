package com.springbootproject.Controller;

import com.springbootproject.Model.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

            // 获取列名
            int columnCount = rs.getMetaData().getColumnCount();

            while (rs.next()) {
                Map<String, Object> roleMap = new HashMap<>();
                // 将每行数据转换为Map
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rs.getMetaData().getColumnName(i);
                    Object value = rs.getObject(i);
                    roleMap.put(columnName, value);
                }
                roleList.add(roleMap);
            }

            return ResponseEntity.ok(ApiResponse.success("获取角色字典成功", roleList));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取角色字典失败: " + e.getMessage()));
        }
    }
}