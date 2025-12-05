package com.springbootproject.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.springbootproject.Entity.UserRoute;
import com.springbootproject.Repository.UserRouteRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户路由服务类
 * 处理用户路由相关的业务逻辑
 */
@Service
public class UserRouteService {

    @Autowired
    private UserRouteRepository userRouteRepository;

    /**
     * 获取用户的所有路由
     * @param userId 用户ID
     * @return 用户路由列表
     */
    public List<UserRoute> getUserRoutes(Long userId) {
        return userRouteRepository.findByUserId(userId);
    }

    /**
     * 获取用户的可见路由
     * @param userId 用户ID
     * @return 用户可见路由列表
     */
    public List<UserRoute> getUserVisibleRoutes(Long userId) {
        return userRouteRepository.findByUserIdAndVisible(userId, true);
    }

    /**
     * 保存用户路由
     * @param route 路由实体
     * @return 保存后的路由实体
     */
    public UserRoute saveRoute(UserRoute route) {
        return userRouteRepository.save(route);
    }

    /**
     * 批量保存用户路由
     * @param routes 路由列表
     * @return 保存后的路由列表
     */
    public List<UserRoute> saveRoutes(List<UserRoute> routes) {
        return userRouteRepository.saveAll(routes);
    }

    /**
     * 删除用户的所有路由
     * @param userId 用户ID
     */
    public void deleteUserRoutes(Long userId) {
        userRouteRepository.deleteByUserId(userId);
    }

    /**
     * 构建树形结构的路由数据（用于多级菜单）
     * @param routes 路由列表
     * @return 树形结构的路由列表
     */
    public List<UserRoute> buildTreeRoutes(List<UserRoute> routes) {
        // 先找出所有顶级路由（parentId为null或0）
        List<UserRoute> rootRoutes = routes.stream()
                .filter(route -> route.getParentId() == null || route.getParentId() == 0)
                .collect(Collectors.toList());
        
        // 为每个顶级路由构建子路由
        for (UserRoute rootRoute : rootRoutes) {
            buildChildrenRoutes(rootRoute, routes);
        }
        
        return rootRoutes;
    }
    
    /**
     * 递归构建子路由
     * @param parentRoute 父路由
     * @param allRoutes 所有路由列表
     */
    private void buildChildrenRoutes(UserRoute parentRoute, List<UserRoute> allRoutes) {
        // 找出当前父路由的所有子路由
        List<UserRoute> childrenRoutes = allRoutes.stream()
                .filter(route -> parentRoute.getId().equals(route.getParentId()))
                .collect(Collectors.toList());
        
        // 为每个子路由继续构建子路由
        for (UserRoute childRoute : childrenRoutes) {
            buildChildrenRoutes(childRoute, allRoutes);
        }
        
        // 设置父路由的子路由列表
        parentRoute.setChildren(childrenRoutes);
    }
}