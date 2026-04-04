package com.springbootproject.Service;

import com.springbootproject.Controller.NativeWebSocketController;
import com.springbootproject.Entity.Friendship;
import com.springbootproject.Entity.User;
import com.springbootproject.Repository.FriendshipRepository;
import com.springbootproject.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FriendshipService {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Friendship sendFriendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new RuntimeException("不能添加自己为好友");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        User friend = userRepository.findById(friendId).orElse(null);
        if (friend == null) {
            throw new RuntimeException("要添加的好友不存在");
        }

        Optional<Friendship> existingFriendship = friendshipRepository.findFriendshipBetweenUsers(userId, friendId);
        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();
            if ("accepted".equals(friendship.getStatus())) {
                throw new RuntimeException("已经是好友了");
            } else if ("pending".equals(friendship.getStatus())) {
                if (friendship.getUserId().equals(userId)) {
                    throw new RuntimeException("已经发送过好友请求，等待对方确认");
                } else {
                    throw new RuntimeException("对方已经向你发送过好友请求，请先处理");
                }
            } else if ("rejected".equals(friendship.getStatus())) {
                friendship.setStatus("pending");
                friendship.setUserId(userId);
                friendship.setFriendId(friendId);
                friendship.setUpdateTime(LocalDateTime.now());
                Friendship saved = friendshipRepository.save(friendship);
                notifyFriendRequest(friendId, user, saved.getId());
                return saved;
            }
        }

        Friendship friendship = new Friendship(userId, friendId);
        Friendship saved = friendshipRepository.save(friendship);
        notifyFriendRequest(friendId, user, saved.getId());
        return saved;
    }
    
    private void notifyFriendRequest(Long toUserId, User fromUser, Long friendshipId) {
        String avatar = fromUser.getAvatar();
        if (avatar == null || avatar.isEmpty()) {
            avatar = "/uploads/avatars/default.png";
        } else if (!avatar.startsWith("/uploads/")) {
            avatar = "/uploads/avatars/" + avatar;
        }
        NativeWebSocketController.sendFriendRequestNotification(
            toUserId, 
            fromUser.getId(), 
            fromUser.getUsername(), 
            avatar, 
            friendshipId
        );
    }

    @Transactional
    public Friendship acceptFriendRequest(Long friendshipId, Long userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("好友请求不存在"));

        if (!friendship.getFriendId().equals(userId)) {
            throw new RuntimeException("无权处理此好友请求");
        }

        if (!"pending".equals(friendship.getStatus())) {
            throw new RuntimeException("该好友请求已经处理过了");
        }

        friendship.setStatus("accepted");
        friendship.setUpdateTime(LocalDateTime.now());
        Friendship saved = friendshipRepository.save(friendship);
        
        User accepter = userRepository.findById(userId).orElse(null);
        if (accepter != null) {
            String avatar = accepter.getAvatar();
            if (avatar == null || avatar.isEmpty()) {
                avatar = "/uploads/avatars/default.png";
            } else if (!avatar.startsWith("/uploads/")) {
                avatar = "/uploads/avatars/" + avatar;
            }
            NativeWebSocketController.sendFriendAcceptedNotification(
                friendship.getUserId(),
                accepter.getId(),
                accepter.getUsername(),
                avatar
            );
        }
        
        return saved;
    }

    @Transactional
    public Friendship rejectFriendRequest(Long friendshipId, Long userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("好友请求不存在"));

        if (!friendship.getFriendId().equals(userId)) {
            throw new RuntimeException("无权处理此好友请求");
        }

        if (!"pending".equals(friendship.getStatus())) {
            throw new RuntimeException("该好友请求已经处理过了");
        }

        friendship.setStatus("rejected");
        friendship.setUpdateTime(LocalDateTime.now());
        return friendshipRepository.save(friendship);
    }

    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        Optional<Friendship> friendship = friendshipRepository.findFriendshipBetweenUsers(userId, friendId);
        if (friendship.isPresent()) {
            friendshipRepository.delete(friendship.get());
        } else {
            throw new RuntimeException("好友关系不存在");
        }
    }

    public List<Map<String, Object>> getFriendList(Long userId) {
        List<Friendship> friendships = friendshipRepository.findFriendsByUserIdAndStatus(userId, "accepted");
        List<Map<String, Object>> friends = new ArrayList<>();

        for (Friendship friendship : friendships) {
            Long friendId = friendship.getUserId().equals(userId) ? friendship.getFriendId() : friendship.getUserId();
            User friend = userRepository.findById(friendId).orElse(null);
            if (friend != null) {
                Map<String, Object> friendInfo = new HashMap<>();
                friendInfo.put("id", friend.getId());
                friendInfo.put("username", friend.getUsername());
                friendInfo.put("avatar", friend.getAvatar());
                friendInfo.put("gender", friend.getGender());
                friendInfo.put("friendshipId", friendship.getId());
                friends.add(friendInfo);
            }
        }

        return friends;
    }

    public List<Map<String, Object>> getPendingRequests(Long userId) {
        List<Friendship> friendships = friendshipRepository.findByFriendIdAndStatus(userId, "pending");
        List<Map<String, Object>> requests = new ArrayList<>();

        for (Friendship friendship : friendships) {
            User requester = userRepository.findById(friendship.getUserId()).orElse(null);
            if (requester != null) {
                Map<String, Object> requestInfo = new HashMap<>();
                requestInfo.put("friendshipId", friendship.getId());
                requestInfo.put("userId", requester.getId());
                requestInfo.put("username", requester.getUsername());
                requestInfo.put("avatar", requester.getAvatar());
                requestInfo.put("createTime", friendship.getCreateTime());
                requests.add(requestInfo);
            }
        }

        return requests;
    }

    public List<Map<String, Object>> getSentRequests(Long userId) {
        List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(userId, "pending");
        List<Map<String, Object>> requests = new ArrayList<>();

        for (Friendship friendship : friendships) {
            User target = userRepository.findById(friendship.getFriendId()).orElse(null);
            if (target != null) {
                Map<String, Object> requestInfo = new HashMap<>();
                requestInfo.put("friendshipId", friendship.getId());
                requestInfo.put("targetUserId", target.getId());
                requestInfo.put("targetUsername", target.getUsername());
                requestInfo.put("targetAvatar", target.getAvatar());
                requestInfo.put("createTime", friendship.getCreateTime());
                requests.add(requestInfo);
            }
        }

        return requests;
    }

    public boolean areFriends(Long userId, Long friendId) {
        return friendshipRepository.areFriends(userId, friendId);
    }

    public Optional<Friendship> getFriendshipBetweenUsers(Long userId, Long friendId) {
        return friendshipRepository.findFriendshipBetweenUsers(userId, friendId);
    }

    public List<Map<String, Object>> searchUsers(Long currentUserId, String keyword) {
        List<User> users;
        if (keyword == null || keyword.trim().isEmpty()) {
            users = userRepository.findAll();
        } else {
            users = userRepository.findByUsernameContaining(keyword);
        }
        
        List<Map<String, Object>> result = new ArrayList<>();

        for (User user : users) {
            if (user.getId().equals(currentUserId)) {
                continue;
            }

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("avatar", user.getAvatar());
            userInfo.put("gender", user.getGender());

            Optional<Friendship> friendship = friendshipRepository.findFriendshipBetweenUsers(currentUserId, user.getId());
            if (friendship.isPresent()) {
                Friendship f = friendship.get();
                String status = f.getStatus();
                if ("accepted".equals(status)) {
                    userInfo.put("friendStatus", "friend");
                } else if ("pending".equals(status)) {
                    if (f.getUserId().equals(currentUserId)) {
                        userInfo.put("friendStatus", "requestSent");
                    } else {
                        userInfo.put("friendStatus", "requestReceived");
                    }
                } else {
                    userInfo.put("friendStatus", "none");
                }
            } else {
                userInfo.put("friendStatus", "none");
            }

            result.add(userInfo);
        }

        return result;
    }

    public List<Map<String, Object>> getRecommendedFriends(Long currentUserId) {
        List<User> allUsers = userRepository.findAll();
        
        List<User> nonFriendUsers = allUsers.stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .filter(user -> {
                    Optional<Friendship> friendship = friendshipRepository.findFriendshipBetweenUsers(currentUserId, user.getId());
                    if (friendship.isPresent()) {
                        String status = friendship.get().getStatus();
                        return !"accepted".equals(status) && !"pending".equals(status);
                    }
                    return true;
                })
                .collect(Collectors.toList());
        
        Collections.shuffle(nonFriendUsers);
        
        int limit = Math.min(5, nonFriendUsers.size());
        List<User> recommendedUsers = nonFriendUsers.subList(0, limit);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : recommendedUsers) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("avatar", user.getAvatar());
            userInfo.put("gender", user.getGender());
            userInfo.put("friendStatus", "none");
            result.add(userInfo);
        }

        return result;
    }
}
