package com.springbootproject.Service;

import com.springbootproject.Entity.User;
import com.springbootproject.Entity.Workbench;
import com.springbootproject.Entity.WorkbenchUser;
import com.springbootproject.Model.WorkbenchAppResponse;
import com.springbootproject.Model.WorkbenchAssignedUserResponse;
import com.springbootproject.Model.WorkbenchRequest;
import com.springbootproject.Model.WorkbenchResponse;
import com.springbootproject.Repository.UserRepository;
import com.springbootproject.Repository.WorkbenchRepository;
import com.springbootproject.Repository.WorkbenchUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 工作台服务实现类
 */
@Service
public class WorkbenchService {

    @Autowired
    private WorkbenchRepository workbenchRepository;

    @Autowired
    private WorkbenchUserRepository workbenchUserRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取所有工作台列表
     * @return 工作台列表
     */
    public List<WorkbenchResponse> getAllWorkbenches() {
        List<Workbench> workbenches = workbenchRepository.findAllByOrderByIdAsc();
        return buildWorkbenchResponses(workbenches);
    }

    /**
     * 创建工作台
     * @param request 工作台请求对象
     * @return 创建后的工作台对象
     */
    @Transactional
    public WorkbenchResponse createWorkbench(WorkbenchRequest request) {
        if (request == null) {
            throw new RuntimeException("工作台数据不能为空");
        }

        Workbench newWorkbench = new Workbench();
        newWorkbench.setName(normalizeName(request.getName()));
        newWorkbench.setIcon(normalizeText(request.getIcon()));
        newWorkbench.setLink(normalizeText(request.getLink()));
        Workbench savedWorkbench = workbenchRepository.save(newWorkbench);

        replaceWorkbenchUsers(savedWorkbench.getId(), request.getUserIds(), true);
        return getWorkbenchResponseById(savedWorkbench.getId());
    }

    /**
     * 更新工作台
     * @param id 工作台ID
     * @param request 工作台请求对象
     * @return 更新后的工作台对象
     */
    @Transactional
    public WorkbenchResponse updateWorkbench(Long id, WorkbenchRequest request) {
        if (request == null) {
            throw new RuntimeException("工作台数据不能为空");
        }

        Optional<Workbench> optional = workbenchRepository.findById(id);
        if (!optional.isPresent()) {
            throw new RuntimeException("工作台不存在");
        }

        Workbench existingWorkbench = optional.get();
        existingWorkbench.setName(normalizeName(request.getName()));
        existingWorkbench.setIcon(normalizeText(request.getIcon()));
        existingWorkbench.setLink(normalizeText(request.getLink()));
        workbenchRepository.save(existingWorkbench);

        replaceWorkbenchUsers(id, request.getUserIds(), false);
        return getWorkbenchResponseById(id);
    }

    /**
     * 删除工作台
     * @param id 工作台ID
     */
    @Transactional
    public void deleteWorkbench(Long id) {
        if (!workbenchRepository.existsById(id)) {
            throw new RuntimeException("工作台不存在");
        }
        workbenchUserRepository.deleteByWorkbenchId(id);
        workbenchRepository.deleteById(id);
    }

    /**
     * 根据当前用户获取工作台子应用列表
     * @param userId 当前用户ID
     * @return 工作台子应用列表
     */
    public List<WorkbenchAppResponse> getWorkbenchAppsByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        List<WorkbenchUser> relations = workbenchUserRepository.findByUserId(userId);
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> workbenchIds = new LinkedHashSet<>();
        for (WorkbenchUser relation : relations) {
            if (relation.getWorkbenchId() != null) {
                workbenchIds.add(relation.getWorkbenchId());
            }
        }

        if (workbenchIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Workbench> workbenches = workbenchRepository.findByIdInOrderByIdAsc(workbenchIds);
        List<WorkbenchAppResponse> responses = new ArrayList<>();
        for (Workbench workbench : workbenches) {
            responses.add(new WorkbenchAppResponse(
                    workbench.getName(),
                    workbench.getIcon(),
                    workbench.getLink()));
        }
        return responses;
    }

    private WorkbenchResponse getWorkbenchResponseById(Long id) {
        Workbench workbench = workbenchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工作台不存在"));
        return buildWorkbenchResponses(Collections.singletonList(workbench)).get(0);
    }

    private List<WorkbenchResponse> buildWorkbenchResponses(List<Workbench> workbenches) {
        if (workbenches == null || workbenches.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> workbenchIds = new LinkedHashSet<>();
        for (Workbench workbench : workbenches) {
            if (workbench.getId() != null) {
                workbenchIds.add(workbench.getId());
            }
        }

        Map<Long, List<WorkbenchAssignedUserResponse>> workbenchUsersMap = buildWorkbenchUsersMap(workbenchIds);
        List<WorkbenchResponse> responses = new ArrayList<>();
        for (Workbench workbench : workbenches) {
            WorkbenchResponse response = new WorkbenchResponse();
            response.setId(workbench.getId());
            response.setName(workbench.getName());
            response.setIcon(workbench.getIcon());
            response.setLink(workbench.getLink());
            response.setUsers(workbenchUsersMap.getOrDefault(workbench.getId(), new ArrayList<>()));
            responses.add(response);
        }
        return responses;
    }

    private Map<Long, List<WorkbenchAssignedUserResponse>> buildWorkbenchUsersMap(Collection<Long> workbenchIds) {
        Map<Long, List<WorkbenchAssignedUserResponse>> result = new HashMap<>();
        if (workbenchIds == null || workbenchIds.isEmpty()) {
            return result;
        }

        List<WorkbenchUser> relations = workbenchUserRepository.findByWorkbenchIdIn(workbenchIds);
        if (relations.isEmpty()) {
            return result;
        }

        Set<Long> userIds = new LinkedHashSet<>();
        for (WorkbenchUser relation : relations) {
            if (relation.getUserId() != null) {
                userIds.add(relation.getUserId());
            }
        }

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User user : userRepository.findAllById(userIds)) {
                userMap.put(user.getId(), user);
            }
        }

        for (WorkbenchUser relation : relations) {
            if (relation.getWorkbenchId() == null || relation.getUserId() == null) {
                continue;
            }
            User user = userMap.get(relation.getUserId());
            if (user == null) {
                continue;
            }
            result.computeIfAbsent(relation.getWorkbenchId(), key -> new ArrayList<>())
                    .add(new WorkbenchAssignedUserResponse(user.getId(), user.getUsername()));
        }
        return result;
    }

    private void replaceWorkbenchUsers(Long workbenchId, List<Long> userIds, boolean clearWhenNull) {
        if (workbenchId == null) {
            return;
        }
        if (userIds == null && !clearWhenNull) {
            return;
        }

        workbenchUserRepository.deleteByWorkbenchId(workbenchId);

        List<Long> normalizedUserIds = normalizeUserIds(userIds);
        if (normalizedUserIds.isEmpty()) {
            return;
        }

        validateUsersExist(normalizedUserIds);
        for (Long userId : normalizedUserIds) {
            WorkbenchUser relation = new WorkbenchUser();
            relation.setWorkbenchId(workbenchId);
            relation.setUserId(userId);
            workbenchUserRepository.save(relation);
        }
    }

    private List<Long> normalizeUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> normalized = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId != null) {
                normalized.add(userId);
            }
        }
        return new ArrayList<>(normalized);
    }

    private void validateUsersExist(List<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);
        Set<Long> existingIds = new HashSet<>();
        for (User user : users) {
            existingIds.add(user.getId());
        }

        List<Long> missingIds = new ArrayList<>();
        for (Long userId : userIds) {
            if (!existingIds.contains(userId)) {
                missingIds.add(userId);
            }
        }

        if (!missingIds.isEmpty()) {
            throw new RuntimeException("以下用户不存在: " + missingIds);
        }
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("工作台名称不能为空");
        }
        return name.trim();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
