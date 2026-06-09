package com.mini.spring.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 绑定 URL 路径变量到方法参数，如 /users/{id}
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring 的 {@code org.springframework.web.bind.annotation.PathVariable}。
 * 用于从 URL 路径中提取变量值，自动进行类型转换。
 * <p>
 * <b>使用示例：</b>
 * <pre>
 *   // 请求：GET /api/users/42
 *   @GetMapping("/users/{id}")
 *   public User getUser(@PathVariable int id) {
 *       // id = 42（自动从 "42" 转为 int）
 *   }
 *
 *   // 多个路径变量：GET /api/users/42/posts/100
 *   @GetMapping("/users/{userId}/posts/{postId}")
 *   public Post getPost(@PathVariable int userId, @PathVariable int postId) { ... }
 * </pre>
 *
 * @see RequestParam 查询参数绑定
 */
@Target(ElementType.PARAMETER)      // 只能标注在方法参数上
@Retention(RetentionPolicy.RUNTIME)  // 运行时保留
public @interface PathVariable {
    /**
     * 路径变量名
     * <p>
     * 对应 URL 中的 {xxx} 占位符。
     * 默认为空字符串，此时使用参数名匹配。
     * 例如：@PathVariable("userId") 匹配 {userId}
     */
    String value() default "";
}
