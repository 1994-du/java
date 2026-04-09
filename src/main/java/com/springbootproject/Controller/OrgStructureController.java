package com.springbootproject.Controller;

import com.springbootproject.Entity.OrgStructure;
import com.springbootproject.Model.ApiResponse;
import com.springbootproject.Service.OrgStructureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 组织结构控制器
 */
@RestController
@RequestMapping("/api/org-structures")
public class OrgStructureController {
    
    @Autowired
    private OrgStructureService orgStructureService;
    
    /**
     * 获取所有组织结构列表
     * @return 组织结构列表
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<OrgStructure>>> getOrgStructures() {
        try {
            List<OrgStructure> structures = orgStructureService.getAllOrgStructures();
            return ResponseEntity.ok(ApiResponse.success("获取组织结构列表成功", structures, (long) structures.size()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取组织结构列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取组织结构树形结构（支持通过parentId查询子节点）
     * @param requestBody 请求体，包含parentId（可选，不传则查询根节点）
     * @return 组织结构树形结构
     */
    @PostMapping("/tree")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrgStructureTree(@RequestBody(required = false) Map<String, Long> requestBody) {
        try {
            Long parentId = null;
            if (requestBody != null && requestBody.containsKey("parentId")) {
                parentId = requestBody.get("parentId");
            }
            List<OrgStructure> structures = orgStructureService.getAllOrgStructures();
            List<Map<String, Object>> tree = orgStructureService.buildOrgStructureTree(structures, parentId);
            return ResponseEntity.ok(ApiResponse.success("获取组织结构树形结构成功", tree));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取组织结构树形结构失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据ID获取组织结构详情
     * @param id 结构ID
     * @return 组织结构详情
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<ApiResponse<OrgStructure>> getOrgStructureById(@PathVariable Long id) {
        try {
            OrgStructure structure = orgStructureService.getOrgStructureById(id);
            if (structure == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("组织结构不存在"));
            }
            return ResponseEntity.ok(ApiResponse.success("获取组织结构详情成功", structure));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取组织结构详情失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建组织结构
     * @param structure 组织结构对象
     * @return 创建的组织结构对象
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<OrgStructure>> createOrgStructure(@RequestBody OrgStructure structure) {
        try {
            OrgStructure createdStructure = orgStructureService.createOrgStructure(structure);
            return ResponseEntity.ok(ApiResponse.success("创建组织结构成功", createdStructure));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("创建组织结构失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新组织结构
     * @param id 结构ID
     * @param structure 组织结构对象
     * @return 更新后的组织结构对象
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<OrgStructure>> updateOrgStructure(@PathVariable Long id, @RequestBody OrgStructure structure) {
        try {
            OrgStructure updatedStructure = orgStructureService.updateOrgStructure(id, structure);
            return ResponseEntity.ok(ApiResponse.success("更新组织结构成功", updatedStructure));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("更新组织结构失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除组织结构
     * @param id 结构ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteOrgStructure(@PathVariable Long id) {
        try {
            orgStructureService.deleteOrgStructure(id);
            return ResponseEntity.ok(ApiResponse.success("删除组织结构成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("删除组织结构失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据父结构ID获取子结构列表
     * @param parentId 父结构ID
     * @return 子结构列表
     */
    @GetMapping("/children/{parentId}")
    public ResponseEntity<ApiResponse<List<OrgStructure>>> getChildOrgStructures(@PathVariable Long parentId) {
        try {
            List<OrgStructure> structures = orgStructureService.getOrgStructuresByParentId(parentId);
            return ResponseEntity.ok(ApiResponse.success("获取子结构列表成功", structures, (long) structures.size()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取子结构列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据状态获取组织结构列表
     * @param status 状态（1: 启用, 0: 禁用）
     * @return 组织结构列表
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<OrgStructure>>> getOrgStructuresByStatus(@PathVariable Integer status) {
        try {
            List<OrgStructure> structures = orgStructureService.getOrgStructuresByStatus(status);
            return ResponseEntity.ok(ApiResponse.success("获取组织结构列表成功", structures, (long) structures.size()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取组织结构列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据是否为叶子节点获取组织结构列表
     * @param isLeaf 是否为叶子节点（1: 是, 0: 否）
     * @return 组织结构列表
     */
    @GetMapping("/leaf/{isLeaf}")
    public ResponseEntity<ApiResponse<List<OrgStructure>>> getOrgStructuresByIsLeaf(@PathVariable Integer isLeaf) {
        try {
            List<OrgStructure> structures = orgStructureService.getOrgStructuresByIsLeaf(isLeaf);
            return ResponseEntity.ok(ApiResponse.success("获取组织结构列表成功", structures, (long) structures.size()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取组织结构列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据层级获取组织结构列表
     * @param level 层级
     * @return 组织结构列表
     */
    @GetMapping("/level/{level}")
    public ResponseEntity<ApiResponse<List<OrgStructure>>> getOrgStructuresByLevel(@PathVariable Integer level) {
        try {
            List<OrgStructure> structures = orgStructureService.getOrgStructuresByLevel(level);
            return ResponseEntity.ok(ApiResponse.success("获取组织结构列表成功", structures, (long) structures.size()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取组织结构列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取组织结构的完整路径
     * @param id 结构ID
     * @return 完整路径
     */
    @GetMapping("/path/{id}")
    public ResponseEntity<ApiResponse<String>> getOrgStructurePath(@PathVariable Long id) {
        try {
            String path = orgStructureService.getOrgStructurePath(id);
            return ResponseEntity.ok(ApiResponse.success("获取组织结构路径成功", path));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("获取组织结构路径失败: " + e.getMessage()));
        }
    }
}