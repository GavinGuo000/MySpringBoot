package com.mini.spring.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * POST 请求映射 —— 将 POST 请求路由到对应的 Controller 方法
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring 的 {@code org.springframework.web.bind.annotation.PostMapping}。
 * 通常用于创建资源或提交数据的场景。
 * <p>
 * <b>使用示例：</b>
 * <pre>
 *   @PostMapping("/users")               // POST /users
 *   public User createUser(@RequestParam String name) { ... }
 * </pre>
 *
 * @see GetMapping GET 请求映射
 */
@Target(ElementType.METHOD)         // 只能标注在方法上
@Retention(RetentionPolicy.RUNTIME)  // 运行时保留
public @interface PostMapping {
    /** 请求路径（如 "/users"） */
    String value() default "";
}
