package com.mini.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 从配置文件注入属性值，支持 ${key:default} 语法
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring 的 {@code org.springframework.beans.factory.annotation.Value}。
 * 用于将 application.properties 中的配置值注入到 Bean 的字段中。
 * <p>
 * <b>使用示例：</b>
 * <pre>
 *   // application.properties:
 *   //   app.name=MyApp
 *   //   app.version=1.0
 *
 *   @Component
 *   public class AppConfig {
 *       @Value("${app.name}")              // 必须存在，否则抛异常
 *       private String appName;
 *
 *       @Value("${app.version:2.0}")       // 不存在时使用默认值 "2.0"
 *       private String version;
 *
 *       @Value("${server.port:8080}")      // 支持类型转换
 *       private int port;
 *   }
 * </pre>
 * <p>
 * <b>处理流程：</b>
 * BeanFactory.populateBean() 扫描到 @Value 注解后，
 * 调用 PropertyResolver.resolve(value(), fieldType) 解析占位符并转换类型。
 *
 * @see com.mini.spring.core.PropertyResolver 解析占位符表达式
 * @see com.mini.spring.core.BeanFactory 在 populateBean() 中处理此注解
 */
@Target(ElementType.FIELD)          // 只能标注在字段上
@Retention(RetentionPolicy.RUNTIME)  // 运行时保留
public @interface Value {
    /**
     * 属性表达式
     * <p>
     * 支持 ${key} 和 ${key:default} 两种语法。
     * 示例：
     * <ul>
     *   <li>"${server.port}" —— 必须在配置文件中定义</li>
     *   <li>"${server.port:8080}" —— 支持默认值</li>
     *   <li>"${app.name:MyApp}" —— 字符串默认值</li>
     * </ul>
     */
    String value();
}
