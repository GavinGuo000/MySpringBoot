package com.mini.demo.service;

import java.util.*;

/**
 * 用户仓库 —— 通过 @Bean 方法注册，而非 @Component 扫描
 */
public class UserRepository {

    private final Map<Integer, Map<String, Object>> users = new LinkedHashMap<>();
    private int idCounter = 1;

    public UserRepository() {
        // 初始化一些测试数据
        save(Map.of("id", idCounter++, "name", "张三", "email", "zhangsan@example.com"));
        save(Map.of("id", idCounter++, "name", "李四", "email", "lisi@example.com"));
        save(Map.of("id", idCounter++, "name", "王五", "email", "wangwu@example.com"));
    }

    public List<Map<String, Object>> findAll() {
        return new ArrayList<>(users.values());
    }

    public Map<String, Object> findById(int id) {
        return users.get(id);
    }

    public void save(Map<String, Object> user) {
        int id = (int) user.get("id");
        users.put(id, user);
    }

    public int nextId() {
        return idCounter++;
    }
}
