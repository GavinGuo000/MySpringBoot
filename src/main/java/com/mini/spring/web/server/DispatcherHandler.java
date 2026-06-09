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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * 请求分发处理器 —— 类似 Spring MVC 的 DispatcherServlet
 * <p>
 * <b>学习要点：</b>
 * DispatcherServlet 是 Spring MVC 的核心，所有 HTTP 请求都先经过它，
 * 再根据 URL 路由到对应的 Controller 方法处理。
 * <p>
 * 它实现了 JDK 内置的 {@code com.sun.net.httpserver.HttpHandler} 接口，
 * 这样可以直接与 JDK 的 HttpServer 配合使用，无需引入 Tomcat。
 * <p>
 * <b>请求处理流程：</b>
 * <pre>
 *   HTTP 请求到达
 *       ↓
 *   handle(HttpExchange) 被调用
 *       ↓
 *   1. 解析请求信息（method, path, queryString）
 *       ↓
 *   2. handlerMapping.match() 查找匹配的路由
 *       ↓
 *   3. resolveMethodArguments() 解析参数绑定（@PathVariable, @RequestParam）
 *       ↓
 *   4. handlerMethod.invoke() 调用 Controller 方法
 *       ↓
 *   5. sendJson() 将返回值序列化为 JSON 响应
 * </pre>
 *
 * @see HandlerMapping 路由映射表
 * @see EmbeddedWebServer 注册此 Handler 到 HTTP 服务器
 */
public class DispatcherHandler implements HttpHandler {

    /** 路由映射表，用于查找 URL 对应的 Controller 方法 */
    private final HandlerMapping handlerMapping;

    /** 过滤器列表（按 order 排序），在请求到达前依次执行 */
    private final List<FilterRegistrationBean> filterRegistrations;

    /** 拦截器注册表，在 Controller 执行前后调用 */
    private final InterceptorRegistry interceptorRegistry;

    /** JSON 序列化/反序列化工具（使用 Google Gson 库） */
    private final Gson gson = new Gson();

    public DispatcherHandler(HandlerMapping handlerMapping) {
        this(handlerMapping, new ArrayList<>(), new InterceptorRegistry());
    }

    public DispatcherHandler(HandlerMapping handlerMapping,
                             List<FilterRegistrationBean> filterRegistrations,
                             InterceptorRegistry interceptorRegistry) {
        this.handlerMapping = handlerMapping;
        this.filterRegistrations = filterRegistrations;
        this.interceptorRegistry = interceptorRegistry;
    }

    /**
     * 处理 HTTP 请求（核心入口方法）
     * <p>
     * 对标 Spring MVC 的 {@code DispatcherServlet.doDispatch()}。
     * 每个 HTTP 请求都会触发此方法，完整流程：
     * <pre>
     *   1. 提取请求信息（HTTP 方法、路径、查询字符串）
     *   2. 设置 CORS 响应头（允许跨域）
     *   3. 通过 HandlerMapping 查找匹配的路由
     *   4. 如果找不到，返回 404
     *   5. 解析请求参数并绑定到方法参数
     *   6. 调用 Controller 方法
     *   7. 将返回值序列化为 JSON 返回
     * </pre>
     *
     * @param exchange JDK HttpServer 提供的请求/响应封装对象
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 提取请求路径，用于过滤器匹配
        String path = exchange.getRequestURI().getPath();

        // 筛选匹配当前路径的过滤器，并按 order 排序
        List<Filter> matchedFilters = filterRegistrations.stream()
                .filter(reg -> reg.matches(path))
                .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
                .map(FilterRegistrationBean::getFilter)
                .collect(Collectors.toList());

        // 构建过滤器链，最终处理器为本类的 doDispatch 方法
        FilterChain chain = new FilterChain(matchedFilters, this::doDispatch);

        // 启动过滤器链执行（Filter → Filter → ... → doDispatch）
        chain.doFilter(exchange);
    }

    /**
     * 核心请求分发逻辑 —— 在过滤器链末端被调用
     * <p>
     * 执行流程：
     * <pre>
     *   1. 解析请求信息
     *   2. 路由匹配（HandlerMapping）
     *   3. 执行拦截器 preHandle
     *   4. 调用 Controller 方法
     *   5. 执行拦截器 postHandle
     *   6. 发送响应
     *   7. 执行拦截器 afterCompletion
     * </pre>
     */
    private void doDispatch(HttpExchange exchange) throws IOException {
        // 提取请求信息
        String method = exchange.getRequestMethod();          // HTTP 方法（GET/POST）
        String path = exchange.getRequestURI().getPath();     // 请求路径（如 /api/users/42）
        String queryString = exchange.getRequestURI().getQuery(); // 查询字符串（如 ?name=tom&age=20）

        // 打印请求日志
        System.out.println("[MiniSpring] " + method + " " + path
                + (queryString != null ? "?" + queryString : ""));

        // 设置 CORS 响应头（允许跨域请求）
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        // 统一设置响应类型为 JSON
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

        // 查找匹配的路由
        HandlerMapping.MatchResult match = handlerMapping.match(method, path);

        // 未找到匹配路由，返回 404
        if (match == null) {
            sendError(exchange, 404, "未找到路由: " + method + " " + path);
            return;
        }

        // 筛选适用于当前路径的拦截器
        List<InterceptorRegistry.InterceptorMapping> applicableInterceptors =
                interceptorRegistry.getMappings().stream()
                        .filter(m -> m.shouldIntercept(path))
                        .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
                        .collect(Collectors.toList());

        // 记录 preHandle 返回 true 的拦截器（用于后续调用 postHandle/afterCompletion）
        List<HandlerInterceptor> preHandled = new ArrayList<>();
        Exception dispatchException = null;

        try {
            // ========= 执行拦截器 preHandle =========
            for (InterceptorRegistry.InterceptorMapping mapping : applicableInterceptors) {
                HandlerInterceptor interceptor = mapping.getInterceptor();
                boolean proceed = interceptor.preHandle(exchange, match.route.handlerMethod);
                if (!proceed) {
                    // 拦截器中断请求，不再继续执行
                    return;
                }
                preHandled.add(interceptor);
            }

            // 解析查询参数（如 "?name=tom&age=20" → {"name":"tom", "age":"20"}）
            Map<String, String> queryParams = parseQueryParams(queryString);

            // 解析 Controller 方法的参数（根据 @PathVariable/@RequestParam 绑定值）
            Method handlerMethod = match.route.handlerMethod;
            Object[] args = resolveMethodArguments(handlerMethod, match.pathVariables, queryParams);

            // 反射调用 Controller 方法
            Object result = handlerMethod.invoke(match.route.controller, args);

            // ========= 执行拦截器 postHandle =========
            for (int i = preHandled.size() - 1; i >= 0; i--) {
                preHandled.get(i).postHandle(exchange, handlerMethod, result);
            }

            // 将返回值序列化为 JSON 并发送响应
            sendJson(exchange, 200, result);

        } catch (Exception e) {
            dispatchException = e;
            e.printStackTrace();
            sendError(exchange, 500, "服务器内部错误: " + e.getMessage());
        } finally {
            // ========= 执行拦截器 afterCompletion（无论成功或异常都执行）=========
            for (int i = preHandled.size() - 1; i >= 0; i--) {
                try {
                    preHandled.get(i).afterCompletion(exchange, match.route.handlerMethod, dispatchException);
                } catch (Exception ex) {
                    System.err.println("[MiniSpring] 拦截器 afterCompletion 异常: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * 解析方法参数 —— 根据注解绑定参数值
     * <p>
     * 对标 Spring MVC 的 {@code HandlerMethodArgumentResolver}。
     * 支持以下参数绑定方式：
     * <ol>
     *   <li><b>@PathVariable</b> —— 从 URL 路径提取变量（如 /users/{id} → id=42）</li>
     *   <li><b>@RequestParam</b> —— 从查询参数提取（如 ?name=tom）</li>
     *   <li><b>Map 参数</b> —— 传入所有查询参数的 Map</li>
     *   <li><b>默认匹配</b> —— 按参数名从路径变量或查询参数中查找</li>
     * </ol>
     * <p>
     * 示例：
     * <pre>
     *   @GetMapping("/users/{id}")
     *   public User getUser(
     *       @PathVariable int id,              ← 从路径变量 id 取值
     *       @RequestParam(defaultValue="1") int page,  ← 从查询参数 page 取值
     *       Map&lt;String, String&gt; allParams     ← 所有查询参数
     *   )
     * </pre>
     *
     * @param method       Controller 处理方法
     * @param pathVariables 路径变量（从 URL 提取）
     * @param queryParams  查询参数（从 URL 查询字符串解析）
     * @return 方法参数数组
     */
    private Object[] resolveMethodArguments(Method method,
                                            Map<String, String> pathVariables,
                                            Map<String, String> queryParams) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();

            // 1. @PathVariable —— 路径变量（如 /users/{id} 中的 id）
            PathVariable pvAnn = param.getAnnotation(PathVariable.class);
            if (pvAnn != null) {
                // 确定变量名：优先用注解指定，否则用参数名
                String varName = pvAnn.value().isEmpty() ? param.getName() : pvAnn.value();
                String value = pathVariables.get(varName);
                args[i] = convertType(value, paramType); // 类型转换（如 String → int）
                continue;
            }

            // 2. @RequestParam —— 查询参数（如 ?name=tom&age=20）
            RequestParam rpAnn = param.getAnnotation(RequestParam.class);
            if (rpAnn != null) {
                String paramName = rpAnn.value().isEmpty() ? param.getName() : rpAnn.value();
                String value = queryParams.get(paramName);
                if (value == null) {
                    // 查询参数不存在，检查默认值
                    if (!rpAnn.defaultValue().isEmpty()) {
                        value = rpAnn.defaultValue();
                    } else if (rpAnn.required()) {
                        // 必填参数缺失，抛异常
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

            // 4. 默认匹配：按参数名从路径变量或查询参数中查找
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
     * <p>
     * 将 URL 查询字符串（如 "name=tom&age=20"）解析为键值对。
     * 对标 Spring 的 {@code ServletRequestParameterPropertySource}。
     *
     * @param query 查询字符串（如 "name=tom&age=20"），可能为 null
     * @return 参数 Map
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;

        // 按 & 分割每个参数对，再按 = 分割键和值
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0], kv[1]);
            }
        }
        return params;
    }

    /**
     * 类型转换 —— 将 String 值转换为指定的 Java 类型
     * <p>
     * 在参数绑定时，HTTP 请求传递的都是 String，需要转为 Java 方法的参数类型。
     * 支持：String、int/Integer、long/Long、boolean/Boolean、double/Double
     *
     * @param value      字符串值
     * @param targetType 目标类型
     * @return 转换后的值
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
     * <p>
     * 对标 Spring MVC 的 {@code @ResponseBody} + {@code HttpMessageConverter}。
     * 将 Java 对象序列化为 JSON，写入 HTTP 响应体。
     *
     * @param exchange   HTTP 请求/响应对象
     * @param statusCode HTTP 状态码（200/404/500）
     * @param body       响应体（Java 对象或字符串）
     */
    private void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        // 如果已经是 String，直接返回；否则用 Gson 序列化为 JSON
        String json = (body instanceof String) ? (String) body : gson.toJson(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * 发送错误响应
     * <p>
     * 返回标准 JSON 格式的错误信息：
     * <pre>
     *   { "error": "错误描述", "status": 404 }
     * </pre>
     *
     * @param exchange   HTTP 请求/响应对象
     * @param statusCode HTTP 错误状态码
     * @param message    错误描述
     */
    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("status", statusCode);
        sendJson(exchange, statusCode, error);
    }
}
