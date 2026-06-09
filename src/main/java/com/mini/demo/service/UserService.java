package com.mini.demo.service;

import com.mini.spring.annotation.Autowired;
import com.mini.spring.annotation.Component;

import java.util.*;

/**
 * 用户服务 —— 展示 @Autowired 依赖注入
 */
@Component
public class UserService implements UserOperations {

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取所有用户
     */
    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 根据 ID 获取用户
     */
    public Map<String, Object> getUserById(int id) {
        return userRepository.findById(id);
    }

    /**
     * 创建用户
     */
    public Map<String, Object> createUser(String name, String email) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", userRepository.nextId());
        user.put("name", name);
        user.put("email", email);
        userRepository.save(user);
        return user;
    }
}
