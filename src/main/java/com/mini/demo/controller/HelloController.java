package com.mini.demo.controller;

import com.mini.demo.service.GreetingService;
import com.mini.demo.service.UserService;
import com.mini.spring.annotation.Autowired;
import com.mini.spring.web.annotation.*;

import java.util.*;

/**
 * 示例 REST 控制器 —— 展示各种路由和参数绑定
 * <p>
 * 测试方式：
 * <pre>
 *   curl http://localhost:8080/
 *   curl http://localhost:8080/hello?name=Spring
 *   curl http://localhost:8080/info
 *   curl http://localhost:8080/users
 *   curl http://localhost:8080/users/1
 *   curl "http://localhost:8080/users/create?name=赵六&email=zhaoliu@test.com"
 * </pre>
 */
@RestController
@RequestMapping("/api")
public class HelloController {

    @Autowired
    private GreetingService greetingService;

    @Autowired
    private UserService userService;

    // ==================== 基础路由 ====================

    /**
     * 首页
     * GET /
     */
    @GetMapping("/")
    public Map<String, Object> index() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Welcome to Mini Spring Boot!");
        result.put("endpoints", List.of(
                "GET  /api/              - 首页",
                "GET  /api/hello?name=   - 问候（带查询参数）",
                "GET  /api/info          - 应用信息",
                "GET  /api/users         - 用户列表",
                "GET  /api/users/{id}    - 用户详情（路径变量）",
                "POST /api/users/create  - 创建用户"
        ));
        return result;
    }

    // ==================== 查询参数绑定 ====================

    /**
     * 问候接口 - 展示 @RequestParam
     * GET /hello?name=xxx
     */
    @GetMapping("/hello")
    public Map<String, Object> hello(
            @RequestParam(value = "name", defaultValue = "World") String name) {
        return greetingService.getGreeting(name);
    }

    // ==================== 简单 GET ====================

    /**
     * 应用信息 - 展示 @Value 属性注入效果
     * GET /info
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        return greetingService.getAppInfo();
    }

    // ==================== 依赖注入演示 ====================

    /**
     * 用户列表 - 展示 @Autowired 注入 UserService -> UserRepository
     * GET /users
     */
    @GetMapping("/users")
    public Map<String, Object> users() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users", userService.getAllUsers());
        result.put("total", userService.getAllUsers().size());
        return result;
    }

    /**
     * 用户详情 - 展示 @PathVariable
     * GET /users/{id}
     */
    @GetMapping("/users/{id}")
    public Map<String, Object> userDetail(@PathVariable("id") int id) {
        Map<String, Object> user = userService.getUserById(id);
        if (user == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "用户不存在");
            error.put("id", id);
            return error;
        }
        return user;
    }

    /**
     * 创建用户 - 展示 POST + @RequestParam
     * POST /users/create?name=xxx&email=xxx
     */
    @PostMapping("/users/create")
    public Map<String, Object> createUser(
            @RequestParam("name") String name,
            @RequestParam("email") String email) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("user", userService.createUser(name, email));
        return result;
    }
}
