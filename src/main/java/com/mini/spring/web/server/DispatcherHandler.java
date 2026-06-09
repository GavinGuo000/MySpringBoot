package com.mini.spring.web.server;

import com.google.gson.Gson;
import com.mini.spring.web.annotation.PathVariable;
import com.mini.spring.web.annotation.RequestParam;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * 请求分发处理器 —— 类似 Spring MVC 的 DispatcherServlet
 * <p>
 * 学习要点：
 * DispatcherServlet 是 Spring MVC 的核心，所有请求都先经过它，
 * 再分发到对应的 Controller 方法处理。
 */
public class DispatcherHandler implements HttpHandler {

    private final HandlerMapping handlerMapping;
    private final Gson gson = new Gson();

    public DispatcherHandler(HandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String queryString = exchange.getRequestURI().getQuery();

        System.out.println("[MiniSpring] " + method + " " + path
                + (queryString != null ? "?" + queryString : ""));

        // CORS 支持
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

        // 查找匹配的路由
        HandlerMapping.MatchResult match = handlerMapping.match(method, path);

        if (match == null) {
            sendError(exchange, 404, "未找到路由: " + method + " " + path);
            return;
        }

        try {
            // 解析请求参数
            Map<String, String> queryParams = parseQueryParams(queryString);

            // 解析方法参数
            Method handlerMethod = match.route.handlerMethod;
            Object[] args = resolveMethodArguments(handlerMethod, match.pathVariables, queryParams);

            // 调用处理方法
            Object result = handlerMethod.invoke(match.route.controller, args);

            // 返回 JSON 响应
            sendJson(exchange, 200, result);

        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 解析方法参数 —— 根据注解绑定参数值
     */
    private Object[] resolveMethodArguments(Method method,
                                            Map<String, String> pathVariables,
                                            Map<String, String> queryParams) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();

            // 1. @PathVariable —— 路径变量
            PathVariable pvAnn = param.getAnnotation(PathVariable.class);
            if (pvAnn != null) {
                String varName = pvAnn.value().isEmpty() ? param.getName() : pvAnn.value();
                String value = pathVariables.get(varName);
                args[i] = convertType(value, paramType);
                continue;
            }

            // 2. @RequestParam —— 查询参数
            RequestParam rpAnn = param.getAnnotation(RequestParam.class);
            if (rpAnn != null) {
                String paramName = rpAnn.value().isEmpty() ? param.getName() : rpAnn.value();
                String value = queryParams.get(paramName);
                if (value == null) {
                    if (!rpAnn.defaultValue().isEmpty()) {
                        value = rpAnn.defaultValue();
                    } else if (rpAnn.required()) {
                        throw new RuntimeException("缺少必填参数: " + paramName);
                    }
                }
                args[i] = convertType(value, paramType);
                continue;
            }

            // 3. Map<String, String> —— 传入所有查询参数
            if (paramType == Map.class) {
                args[i] = queryParams;
                continue;
            }

            // 4. 默认尝试从路径变量或查询参数中匹配
            String name = param.getName();
            if (pathVariables.containsKey(name)) {
                args[i] = convertType(pathVariables.get(name), paramType);
            } else if (queryParams.containsKey(name)) {
                args[i] = convertType(queryParams.get(name), paramType);
            }
        }

        return args;
    }

    /**
     * 解析查询字符串为 Map
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;

        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0], kv[1]);
            }
        }
        return params;
    }

    /**
     * 类型转换
     */
    private Object convertType(String value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType == String.class) return value;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(value);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(value);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(value);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(value);
        return value;
    }

    /**
     * 发送 JSON 响应
     */
    private void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        String json = (body instanceof String) ? (String) body : gson.toJson(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * 发送错误响应
     */
    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("status", statusCode);
        sendJson(exchange, statusCode, error);
    }
}
