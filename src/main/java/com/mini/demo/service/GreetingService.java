package com.mini.demo.service;

import com.mini.spring.annotation.Component;
import com.mini.spring.annotation.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 示例 Service —— 展示 @Component 和 @Value 的使用
 */
@Component
public class GreetingService {

    @Value("${app.name:Mini Spring Boot}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    /**
     * 获取问候语
     */
    public Map<String, Object> getGreeting(String name) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Hello, " + (name != null ? name : "World") + "!");
        result.put("app", appName);
        result.put("version", appVersion);
        result.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return result;
    }

    /**
     * 获取应用信息
     */
    public Map<String, Object> getAppInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", appName);
        info.put("version", appVersion);
        info.put("java", System.getProperty("java.version"));
        info.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        return info;
    }
}
