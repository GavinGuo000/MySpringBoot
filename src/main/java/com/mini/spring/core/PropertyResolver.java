package com.mini.spring.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 属性解析器 —— 加载 application.properties 并解析 ${key:default} 占位符表达式
 * <p>
 * <b>学习要点：</b>
 * 这是 Spring 的 {@code Environment} / {@code PropertySources} 机制的简化版。
 * 在 Spring 中，{@code @Value("${server.port:8080}")} 能够自动解析配置值，
 * 底层就是由类似的 PropertyResolver 完成的。
 * <p>
 * <b>核心功能：</b>
 * <ol>
 *   <li><b>加载配置文件</b> —— 从 classpath 读取 application.properties</li>
 *   <li><b>占位符解析</b> —— 解析 {@code ${key:default}} 格式的表达式</li>
 *   <li><b>类型转换</b> —— 将 String 值转换为 int/boolean/double 等基本类型</li>
 * </ol>
 * <p>
 * <b>占位符语法：</b>
 * <pre>
 *   ${server.port}           —— 必须在配置文件中定义，否则抛异常
 *   ${server.port:8080}      —— 如果未定义，使用默认值 8080
 *   普通字符串               —— 直接返回原值（不解析）
 * </pre>
 *
 * @see BeanFactory 在 @Value 注入时使用此解析器
 */
public class PropertyResolver {

    /**
     * 占位符正则表达式
     * <p>
     * 匹配格式：${key} 或 ${key:defaultValue}
     * <ul>
     *   <li>group(1) —— key 部分（必须，不含 : 和 } 的字符）</li>
     *   <li>group(2) —— 默认值部分（可选，: 后面的内容）</li>
     * </ul>
     * 示例：
     * <pre>
     *   "${server.port:8080}" → group(1)="server.port", group(2)="8080"
     *   "${app.name}"         → group(1)="app.name", group(2)=null
     * </pre>
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}");

    /**
     * 存储从配置文件加载的所有键值对
     * <p>
     * Java 原生的 Properties 类，内部用 Hashtable 存储，
     * 支持 getProperty(key) / getProperty(key, default) 等方法
     */
    private final Properties properties = new Properties();

    /**
     * 构造时自动加载 application.properties
     * <p>
     * 与 Spring Boot 类似，默认从 classpath 根目录加载 application.properties
     */
    public PropertyResolver() {
        loadProperties("application.properties");
    }

    /**
     * 从 classpath 加载配置文件
     * <p>
     * 使用 ClassLoader.getResourceAsStream() 读取 classpath 下的资源文件。
     * 如果文件不存在或加载失败，会打印警告但不会抛异常（应用仍可运行）。
     *
     * @param filename 配置文件名（如 "application.properties"）
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
     * 解析占位符表达式，返回 String 值
     * <p>
     * 支持 {@code ${key:default}} 语法：
     * <ul>
     *   <li>如果输入不是占位符（不含 ${}），直接返回原值</li>
     *   <li>如果配置文件中存在该 key，返回配置值</li>
     *   <li>如果不存在但有默认值（:后面），返回默认值</li>
     *   <li>如果都不存在，抛出 RuntimeException</li>
     * </ul>
     *
     * @param expression 表达式，如 "${server.port:8080}" 或普通字符串
     * @return 解析后的值
     * @throws RuntimeException 当无法解析且没有默认值时
     */
    public String resolve(String expression) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            return expression; // 不是占位符，直接返回原值
        }

        // 提取 key 和默认值
        String key = matcher.group(1);          // 例如 "server.port"
        String defaultValue = matcher.group(2); // 例如 "8080"，可能为 null

        // 优先从配置文件查找
        String value = properties.getProperty(key);
        if (value != null) {
            return value;
        }
        // 配置文件中没有，使用默认值
        if (defaultValue != null) {
            return defaultValue;
        }
        // 既没有配置也没有默认值，抛异常
        throw new RuntimeException("无法解析属性: " + key + "，且没有默认值");
    }

    /**
     * 解析占位符表达式，并转换为指定类型
     * <p>
     * 在 @Value 字段注入时会用到此方法，例如：
     * <pre>
     *   @Value("${server.port:8080}")
     *   private int port;  // targetType = int.class，会自动转换为 Integer
     * </pre>
     * <p>
     * 支持的类型：String、int/Integer、long/Long、boolean/Boolean、double/Double
     *
     * @param expression 占位符表达式
     * @param targetType 目标类型（字段的 Class 对象）
     * @return 解析并转换后的值
     */
    public Object resolve(String expression, Class<?> targetType) {
        String value = resolve(expression);

        // 根据目标类型进行转换
        if (targetType == String.class) return value;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(value);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(value);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(value);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(value);

        return value; // 其他类型返回原始 String
    }

    /**
     * 直接获取属性值（不做占位符解析）
     * <p>
     * 与 resolve() 不同，这个方法直接按 key 查找，不支持 ${} 语法。
     *
     * @param key 属性键
     * @return 属性值，不存在时返回 null
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * 直接获取属性值，带默认值
     *
     * @param key          属性键
     * @param defaultValue 默认值（当 key 不存在时返回）
     * @return 属性值或默认值
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
