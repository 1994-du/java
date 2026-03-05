package com.springbootproject.Controller;

import com.springbootproject.Entity.Menu;
import com.springbootproject.Model.ApiResponse;
import com.springbootproject.Service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class MenuController {

    @Autowired
    private MenuService menuService;

    /**
     * 获取所有菜单（树形结构）
     * @return 树形结构的菜单列表
     */
    @GetMapping("/getMenus")
    public ResponseEntity<ApiResponse<List<Menu>>> getMenus() {
        try {
            List<Menu> menus = menuService.getAllMenus();
            // 构建树形结构
            List<Menu> menuTree = menuService.buildMenuTree(menus, 0L);
            // 获取总条数
            Long total = (long) menus.size();
            return ResponseEntity.ok(ApiResponse.success("获取菜单成功", menuTree, total));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取菜单失败: " + e.getMessage()));
        }
    }

    /**
     * 获取菜单树
     * @return 树形结构的菜单列表
     */
    @GetMapping("/getMenuTree")
    public ResponseEntity<ApiResponse<List<Menu>>> getMenuTree() {
        try {
            List<Menu> menuTree = menuService.getMenuTree();
            return ResponseEntity.ok(ApiResponse.success("获取菜单树成功", menuTree));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取菜单树失败: " + e.getMessage()));
        }
    }
    
    /**
     * 添加菜单
     * @param menu 菜单对象
     * @return 添加后的菜单对象
     */
    @PostMapping("/addMenu")
    public ResponseEntity<ApiResponse<Menu>> addMenu(@RequestBody Menu menu) {
        try {
            // 验证必填字段
            if (menu.getName() == null || menu.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("菜单名称不能为空"));
            }
            if (menu.getPath() == null || menu.getPath().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("菜单路径不能为空"));
            }
            
            // 设置默认值
            if (menu.getComponent() == null || menu.getComponent().trim().isEmpty()) {
                menu.setComponent("");
            }
            if (menu.getSort() == null) {
                menu.setSort(0);
            }
            
            // 保存菜单
            Menu savedMenu = menuService.saveMenu(menu);
            return ResponseEntity.ok(ApiResponse.success("添加菜单成功", savedMenu));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("添加菜单失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除菜单
     * @param id 菜单ID
     * @return 删除结果
     */
    @PostMapping("/deleteMenu")
    public ResponseEntity<ApiResponse<Void>> deleteMenu(@RequestBody Map<String, Long> request) {
        try {
            Long id = request.get("id");
            if (id == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("菜单ID不能为空"));
            }
            
            // 检查菜单是否存在
            if (!menuService.getMenuById(id).isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("菜单不存在"));
            }
            
            // 检查是否有子菜单
            List<Menu> childMenus = menuService.getChildMenus(id);
            if (!childMenus.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("该菜单下存在子菜单，无法删除"));
            }
            
            // 删除菜单
            menuService.deleteMenu(id);
            return ResponseEntity.ok(ApiResponse.success("删除菜单成功", null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("删除菜单失败: " + e.getMessage()));
        }
    }
    
    /**
     * 修改菜单
     * @param menu 菜单对象
     * @return 修改结果
     */
    @PostMapping("/setMenu")
    public ResponseEntity<ApiResponse<Object>> setMenu(@RequestBody Menu menu) {
        try {
            System.out.println("开始修改菜单，ID: " + menu.getId());
            
            // 验证菜单ID
            if (menu.getId() == null) {
                System.out.println("菜单ID为空");
                return ResponseEntity.badRequest().body(ApiResponse.error("菜单ID不能为空"));
            }
            
            // 检查菜单是否存在
            Optional<Menu> existingMenu = menuService.getMenuById(menu.getId());
            if (!existingMenu.isPresent()) {
                System.out.println("菜单不存在，ID: " + menu.getId());
                return ResponseEntity.badRequest().body(ApiResponse.error("菜单不存在"));
            }
            
            // 验证必填字段
            if (menu.getName() == null || menu.getName().trim().isEmpty()) {
                System.out.println("菜单名称为空");
                return ResponseEntity.badRequest().body(ApiResponse.error("菜单名称不能为空"));
            }
            if (menu.getPath() == null || menu.getPath().trim().isEmpty()) {
                System.out.println("菜单路径为空");
                return ResponseEntity.badRequest().body(ApiResponse.error("菜单路径不能为空"));
            }
            
            // 获取原有菜单数据
            Menu oldMenu = existingMenu.get();
            System.out.println("获取到原有菜单数据: ID=" + oldMenu.getId() + ", name=" + oldMenu.getName() + ", path=" + oldMenu.getPath());
            
            // 只更新用户提供的字段
            oldMenu.setName(menu.getName());
            oldMenu.setPath(menu.getPath());
            System.out.println("更新菜单名称和路径: name=" + menu.getName() + ", path=" + menu.getPath());
            
            // 只更新非null的字段
            if (menu.getComponent() != null) {
                oldMenu.setComponent(menu.getComponent());
                System.out.println("更新菜单组件: " + menu.getComponent());
            }
            if (menu.getMeta() != null) {
                oldMenu.setMeta(menu.getMeta());
                System.out.println("更新菜单元数据: " + menu.getMeta());
            }
            if (menu.getIcon() != null) {
                oldMenu.setIcon(menu.getIcon());
                System.out.println("更新菜单图标: " + menu.getIcon());
            }
            if (menu.getParentId() != null) {
                oldMenu.setParentId(menu.getParentId());
                System.out.println("更新父菜单ID: " + menu.getParentId());
            }
            if (menu.getSort() != null) {
                oldMenu.setSort(menu.getSort());
                System.out.println("更新菜单排序: " + menu.getSort());
            }
            
            // 保存菜单
            System.out.println("开始保存菜单");
            Menu savedMenu = menuService.saveMenu(oldMenu);
            System.out.println("保存菜单成功，ID: " + savedMenu.getId());
            
            return ResponseEntity.ok(ApiResponse.success("修改菜单成功", null));
        } catch (Exception e) {
            System.out.println("修改菜单失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("修改菜单失败: " + e.getMessage()));
        }
    }
}