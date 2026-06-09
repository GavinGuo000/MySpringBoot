package com.mini.spring.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 属性解析器 —— 加载 application.properties 并解析 ${key:default} 表达式
 * <p>
 * 学习要点：Spring 的 Environment / PropertySources 机制的简化版，
 * 理解占位符解析的原理。
 */
public class PropertyResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}");

    private final Properties properties = new Properties();

    public PropertyResolver() {
        loadProperties("application.properties");
    }

    /**
     * 从 classpath 加载配置文件
     */
    private void loadProperties(String filename) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (is != null) {
                properties.load(is);
                System.out.println("[MiniSpring] 已加载配置文件: " + filename);
            } else {
                System.out.println("[MiniSpring] 未找到配置文件: " + filename + "，使用默认值");
            }
        } catch (IOException e) {
            System.out.println("[MiniSpring] 配置文件加载失败: " + e.getMessage());
        }
    }

    /**
     * 获取属性值，支持 ${key:default} 语法
     *
     * @param expression 表达式，如 "${server.port:8080}"
     * @return 解析后的值
     */
    public String resolve(String expression) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            return expression; // 不是占位符，直接返回
        }

        String key = matcher.group(1);
        String defaultValue = matcher.group(2); // 可能为 null

        String value = properties.getProperty(key);
        if (value != null) {
            return value;
        }
        if (defaultValue != null) {
            return defaultValue;
        }
        throw new RuntimeException("无法解析属性: " + key + "，且没有默认值");
    }

    /**
     * 获取属性值并转换为指定类型
     */
    public Object resolve(String expression, Class<?> targetType) {
        String value = resolve(expression);

        if (targetType == String.class) return value;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(value);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(value);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(value);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(value);

        return value;
    }

    /**
     * 直接获取属性值
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
