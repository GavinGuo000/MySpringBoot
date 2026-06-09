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
 * 学习要点：
 * 这是 Spring MVC DispatcherServlet 的核心 —— HandlerMapping 的简化版。
 * 理解 Spring MVC 是如何将 URL 路由到具体的 Controller 方法的。
 */
public class HandlerMapping {

    /**
     * 路由条目
     */
    public static class Route {
        public final String method;       // GET, POST
        public final String pathPattern;  // /api/users/{id}
        public final Pattern regex;       // 编译后的正则
        public final List<String> pathVarNames; // 路径变量名列表
        public final Object controller;   // 控制器实例
        public final Method handlerMethod; // 处理方法

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

    private final List<Route> routes = new ArrayList<>();

    /**
     * 注册控制器 —— 扫描控制器中所有的请求处理方法
     *
     * @param controller RestController 实例
     */
    public void registerController(Object controller) {
        Class<?> clazz = controller.getClass();

        // 获取类级别的 @RequestMapping 作为前缀
        String classPrefix = "";
        RequestMapping classMapping = clazz.getAnnotation(RequestMapping.class);
        if (classMapping != null) {
            classPrefix = normalize(classMapping.value());
        }

        // 扫描所有方法
        for (Method method : clazz.getDeclaredMethods()) {
            String httpMethod = null;
            String methodPath = "";

            if (method.isAnnotationPresent(GetMapping.class)) {
                httpMethod = "GET";
                methodPath = method.getAnnotation(GetMapping.class).value();
            } else if (method.isAnnotationPresent(PostMapping.class)) {
                httpMethod = "POST";
                methodPath = method.getAnnotation(PostMapping.class).value();
            } else if (method.isAnnotationPresent(RequestMapping.class)) {
                httpMethod = "GET"; // 默认 GET
                methodPath = method.getAnnotation(RequestMapping.class).value();
            }

            if (httpMethod == null) continue;

            String fullPath = classPrefix + normalize(methodPath);

            // 解析路径变量，如 /users/{id} -> /users/([^/]+)
            List<String> pathVarNames = new ArrayList<>();
            String regexStr = fullPath;
            Matcher varMatcher = Pattern.compile("\\{([^}]+)}").matcher(fullPath);
            while (varMatcher.find()) {
                pathVarNames.add(varMatcher.group(1));
                regexStr = regexStr.replace("{" + varMatcher.group(1) + "}", "([^/]+)");
            }
            Pattern regex = Pattern.compile("^" + regexStr + "$");

            method.setAccessible(true);
            Route route = new Route(httpMethod, fullPath, regex, pathVarNames, controller, method);
            routes.add(route);

            System.out.println("[MiniSpring] 注册路由: " + httpMethod + " " + fullPath
                    + " -> " + clazz.getSimpleName() + "#" + method.getName());
        }
    }

    /**
     * 根据 HTTP 方法和路径查找匹配的路由
     *
     * @return 匹配的路由和路径变量值
     */
    public MatchResult match(String httpMethod, String path) {
        for (Route route : routes) {
            if (!route.method.equalsIgnoreCase(httpMethod)) continue;

            Matcher matcher = route.regex.matcher(path);
            if (matcher.matches()) {
                Map<String, String> pathVars = new HashMap<>();
                for (int i = 0; i < route.pathVarNames.size(); i++) {
                    pathVars.put(route.pathVarNames.get(i), matcher.group(i + 1));
                }
                return new MatchResult(route, pathVars);
            }
        }
        return null;
    }

    /**
     * 获取所有已注册的路由
     */
    public List<Route> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    /**
     * 规范化路径（确保以 / 开头，不以 / 结尾）
     */
    private String normalize(String path) {
        if (path == null || path.isEmpty()) return "";
        if (!path.startsWith("/")) path = "/" + path;
        if (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
        return path;
    }

    /**
     * 匹配结果
     */
    public static class MatchResult {
        public final Route route;
        public final Map<String, String> pathVariables;

        public MatchResult(Route route, Map<String, String> pathVariables) {
            this.route = route;
            this.pathVariables = pathVariables;
        }
    }
}
