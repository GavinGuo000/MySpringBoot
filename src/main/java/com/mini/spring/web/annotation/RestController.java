package com.mini.spring.web.annotation;

import com.mini.spring.annotation.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个类为 REST 控制器，自动序列化为 JSON
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring 的 {@code org.springframework.web.bind.annotation.RestController}。
 * 它是 @Component 的派生注解，因此标注了 @RestController 的类会：
 * <ol>
 *   <li>被 ClassPathBeanDefinitionScanner 扫描到（派生注解识别机制）</li>
 *   <li>被 MySpringApplication 注册到 HandlerMapping 的路由表中</li>
 * </ol>
 * <p>
 * <b>使用示例：</b>
 * <pre>
 *   @RestController
 *   @RequestMapping("/api")
 *   public class UserController {
 *       @GetMapping("/users/{id}")
 *       public User getUser(@PathVariable int id) { ... }
 *   }
 * </pre>
 *
 * @see RequestMapping 类级别路径前缀
 * @see GetMapping GET 请求映射
 * @see PostMapping POST 请求映射
 */
@Target(ElementType.TYPE)           // 只能标注在类上
@Retention(RetentionPolicy.RUNTIME)  // 运行时保留
@Component  // 控制器本身也是组件（派生注解）
public @interface RestController {
    /** Bean 名称（默认为类名首字母小写） */
    String value() default "";
}
