package com.mini.demo.service;

import java.util.List;
import java.util.Map;

/**
 * 用户操作接口 —— 定义 UserService 的公开方法
 * <p>
 * AOP 使用 JDK 动态代理时，目标类必须实现接口。
 * 代理对象会实现此接口，因此注入时应使用接口类型。
 */
public interface UserOperations {

    /**
     * 获取所有用户
     */
    List<Map<String, Object>> getAllUsers();

    /**
     * 根据 ID 获取用户
     */
    Map<String, Object> getUserById(int id);

    /**
     * 创建用户
     */
    Map<String, Object> createUser(String name, String email);
}
