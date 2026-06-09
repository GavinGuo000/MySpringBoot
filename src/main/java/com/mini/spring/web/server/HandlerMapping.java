package com.mini.spring.web.server;

import com.mini.spring.web.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 请求处理器映射 —— 扫描 @RestController 并建立 URL -> 方法的映射关系
 * <p>
 * <b>学习要点：</b>
 * 这是 Spring MVC 的 {@code HandlerMapping} + {@code RequestMappingHandlerMapping} 的简化版。
 * 在 Spring MVC 中，DispatcherServlet 通过 HandlerMapping 找到对应的 Controller 方法。
 * <p>
 * <b>核心功能：</b>
 * <ol>
 *   <li><b>路由注册</b> —— 扫描 @RestController 中的 @GetMapping/@PostMapping，建立路由表</li>
 *   <li><b>路径变量支持</b> —— 将 {@code /users/{id}} 转为正则表达式进行匹配</li>
 *   <li><b>路由查找</b> —— 根据 HTTP 方法和 URL 查找匹配的路由</li>
 * </ol>
 * <p>
 * <b>路由注册示例：</b>
 * <pre>
 *   @RestController
 *   @RequestMapping("/api")
 *   public class UserController {
 *       @GetMapping("/users/{id}")       ← 注册为 GET /api/users/{id}
 *       public User getUser(@PathVariable int id) { ... }
 *   }
 *       ↓
 *   Route {
 *       method = "GET",
 *       pathPattern = "/api/users/{id}",
 *       regex = ^/api/users/([^/]+)$,     ← 编译后的正则
 *       pathVarNames = ["id"],              ← 路径变量名
 *       controller = UserController 实例,
 *       handlerMethod = getUser(int)
 *   }
 * </pre>
 *
 * @see DispatcherHandler 使用 HandlerMapping 进行请求分发
 * @see EmbeddedWebServer 启动时创建 DispatcherHandler
 */
public class HandlerMapping {

    /**
     * 路由条目 —— 描述一个 URL 到 Controller 方法的映射
     * <p>
     * 对标 Spring MVC 的 {@code RequestMappingInfo} + {@code HandlerMethod}
     */
    public static class Route {
        /** HTTP 方法（GET / POST） */
        public final String method;
        /** 原始路径模式（如 "/api/users/{id}"） */
        public final String pathPattern;
        /** 路径变量转为正则（如 "/api/users/{id}" → "^/api/users/([^/]+)$"） */
        public final Pattern regex;
        /** 路径变量名列表（如 ["id"]） */
        public final List<String> pathVarNames;
        /** 控制器实例（带 @RestController 的 Bean） */
        public final Object controller;
        /** 处理方法（带 @GetMapping/@PostMapping 的 Method） */
        public final Method handlerMethod;

        public Route(String method, String pathPattern, Pattern regex,
                     List<String> pathVarNames, Object controller, Method handlerMethod) {
            this.method = method;
            this.pathPattern = pathPattern;
            this.regex = regex;
            this.pathVarNames = pathVarNames;
            this.controller = controller;
            this.handlerMethod = handlerMethod;
        }
    }

    /** 所有已注册的路由表 */
    private final List<Route> routes = new ArrayList<>();

    /**
     * 注册控制器 —— 扫描控制器中所有的请求处理方法
     * <p>
     * 对标 Spring 的 {@code RequestMappingHandlerMapping.registerHandler()}。
     * 扫描流程：
     * <pre>
     *   1. 读取类上的 @RequestMapping 作为路径前缀
     *   2. 遍历所有方法，找带 @GetMapping/@PostMapping/@RequestMapping 的
     *   3. 拼接完整路径（前缀 + 方法路径）
     *   4. 解析路径变量 {xxx} → 正则表达式
     *   5. 创建 Route 对象加入路由表
     * </pre>
     *
     * @param controller RestController 实例
     */
    public void registerController(Object controller) {
        Class<?> clazz = controller.getClass();

        // 第 1 步：获取类级别的 @RequestMapping 作为路径前缀
        // 例如 @RequestMapping("/api") → classPrefix = "/api"
        String classPrefix = "";
        RequestMapping classMapping = clazz.getAnnotation(RequestMapping.class);
        if (classMapping != null) {
            classPrefix = normalize(classMapping.value());
        }

        // 第 2 步：遍历控制器的所有方法
        for (Method method : clazz.getDeclaredMethods()) {
            String httpMethod = null;
            String methodPath = "";

            // 检查方法上的路由注解，确定 HTTP 方法和路径
            if (method.isAnnotationPresent(GetMapping.class)) {
                httpMethod = "GET";
                methodPath = method.getAnnotation(GetMapping.class).value();
            } else if (method.isAnnotationPresent(PostMapping.class)) {
                httpMethod = "POST";
                methodPath = method.getAnnotation(PostMapping.class).value();
            } else if (method.isAnnotationPresent(RequestMapping.class)) {
                httpMethod = "GET"; // @RequestMapping 默认处理 GET
                methodPath = method.getAnnotation(RequestMapping.class).value();
            }

            // 没有路由注解的方法，跳过
            if (httpMethod == null) continue;

            // 第 3 步：拼接完整路径（类前缀 + 方法路径）
            String fullPath = classPrefix + normalize(methodPath);

            // 第 4 步：解析路径变量
            // 例如 /users/{id}/posts/{postId} 会提取出 ["id", "postId"]
            // 并将路径转为正则：/users/([^/]+)/posts/([^/]+)
            List<String> pathVarNames = new ArrayList<>();
            String regexStr = fullPath;
            Matcher varMatcher = Pattern.compile("\\{([^}]+)}").matcher(fullPath);
            while (varMatcher.find()) {
                pathVarNames.add(varMatcher.group(1)); // 变量名
                regexStr = regexStr.replace("{" + varMatcher.group(1) + "}", "([^/]+)");
            }
            // 编译正则，加上首尾锚点确保精确匹配
            Pattern regex = Pattern.compile("^" + regexStr + "$");

            // 第 5 步：创建 Route 并加入路由表
            method.setAccessible(true); // 允许调用 private 方法
            Route route = new Route(httpMethod, fullPath, regex, pathVarNames, controller, method);
            routes.add(route);

            System.out.println("[MiniSpring] 注册路由: " + httpMethod + " " + fullPath
                    + " -> " + clazz.getSimpleName() + "#" + method.getName());
        }
    }

    /**
     * 根据 HTTP 方法和路径查找匹配的路由
     * <p>
     * 对标 Spring MVC 的 {@code HandlerMapping.getHandler()}。
     * 遍历路由表，用正则表达式匹配请求路径，并提取路径变量值。
     * <p>
     * 示例：
     * <pre>
     *   请求：GET /api/users/42
     *   路由：GET /api/users/{id}
     *   匹配结果：pathVariables = {"id": "42"}
     * </pre>
     *
     * @param httpMethod HTTP 方法（GET/POST）
     * @param path       请求路径（如 "/api/users/42"）
     * @return 匹配结果（包含路由和路径变量），未匹配返回 null
     */
    public MatchResult match(String httpMethod, String path) {
        for (Route route : routes) {
            // 先检查 HTTP 方法是否匹配
            if (!route.method.equalsIgnoreCase(httpMethod)) continue;

            // 用正则表达式匹配路径
            Matcher matcher = route.regex.matcher(path);
            if (matcher.matches()) {
                // 提取路径变量值
                Map<String, String> pathVars = new HashMap<>();
                for (int i = 0; i < route.pathVarNames.size(); i++) {
                    pathVars.put(route.pathVarNames.get(i), matcher.group(i + 1));
                }
                return new MatchResult(route, pathVars);
            }
        }
        return null; // 没有匹配的路由
    }

    /**
     * 获取所有已注册的路由（只读）
     *
     * @return 路由列表
     */
    public List<Route> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    /**
     * 规范化路径
     * <p>
     * 确保路径以 / 开头，不以 / 结尾（除了根路径 "/"）。
     * 例如：
     * <ul>
     *   <li>"users" → "/users"</li>
     *   <li>"/users/" → "/users"</li>
     *   <li>"" → ""</li>
     * </ul>
     *
     * @param path 原始路径
     * @return 规范化后的路径
     */
    private String normalize(String path) {
        if (path == null || path.isEmpty()) return "";
        if (!path.startsWith("/")) path = "/" + path;
        if (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
        return path;
    }

    /**
     * 匹配结果 —— 包含匹配的路由和提取出的路径变量
     * <p>
     * 对标 Spring MVC 的 {@code HandlerExecutionChain}
     */
    public static class MatchResult {
        /** 匹配的路由 */
        public final Route route;
        /** 路径变量键值对（如 {"id": "42"}） */
        public final Map<String, String> pathVariables;

        public MatchResult(Route route, Map<String, String> pathVariables) {
            this.route = route;
            this.pathVariables = pathVariables;
        }
    }
}
