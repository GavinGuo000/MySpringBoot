package com.mini.spring.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GET 请求映射 —— 将 GET 请求路由到对应的 Controller 方法
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring 的 {@code org.springframework.web.bind.annotation.GetMapping}。
 * <p>
 * <b>使用示例：</b>
 * <pre>
 *   @GetMapping("/hello")                // GET /hello
 *   public String hello() { return "Hi"; }
 *
 *   @GetMapping("/users/{id}")           // GET /users/42（支持路径变量）
 *   public User getUser(@PathVariable int id) { ... }
 * </pre>
 *
 * @see PostMapping POST 请求映射
 * @see RequestMapping 通用请求映射
 */
@Target(ElementType.METHOD)         // 只能标注在方法上
@Retention(RetentionPolicy.RUNTIME)  // 运行时保留
public @interface GetMapping {
    /** 请求路径（如 "/hello" 或 "/users/{id}"） */
    String value() default "";
}
