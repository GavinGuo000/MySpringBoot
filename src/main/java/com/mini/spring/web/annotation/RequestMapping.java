package com.mini.spring.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类级别或方法级别的请求路径映射
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring 的 {@code org.springframework.web.bind.annotation.RequestMapping}。
 * <p>
 * <b>两种用途：</b>
 * <ol>
 *   <li><b>类级别</b> —— 定义路径前缀，该类下所有方法的完整路径 = 前缀 + 方法路径</li>
 *   <li><b>方法级别</b> —— 定义具体路径（默认处理 GET 请求）</li>
 * </ol>
 * <p>
 * <b>使用示例：</b>
 * <pre>
 *   @RestController
 *   @RequestMapping("/api")              // 类级别：路径前缀
 *   public class UserController {
 *       @GetMapping("/users")             // 完整路径：GET /api/users
 *       public List&lt;User&gt; getUsers() { ... }
 *   }
 * </pre>
 *
 * @see GetMapping GET 请求映射
 * @see PostMapping POST 请求映射
 */
@Target({ElementType.TYPE, ElementType.METHOD}) // 可用于类和方法
@Retention(RetentionPolicy.RUNTIME)              // 运行时保留
public @interface RequestMapping {
    /** 请求路径（类上为前缀，方法上为完整路径） */
    String value() default "";
}
