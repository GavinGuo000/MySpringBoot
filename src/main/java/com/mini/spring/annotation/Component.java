package com.mini.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个类为 Spring 组件，IoC 容器会自动扫描并注册
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring 的 {@code org.springframework.stereotype.Component}。
 * 这是 Spring 最基础的注解，其他注解（如 @Service、@Repository、@Controller）
 * 都是它的「派生注解」（在注解上标注 @Component）。
 * <p>
 * <b>工作原理：</b>
 * <pre>
 *   @Component                  ← 标注在类上
 *   public class UserService {}
 *       ↓
 *   ClassPathBeanDefinitionScanner 扫描时识别此注解
 *       ↓
 *   创建 BeanDefinition 并注册到 BeanFactory
 *       ↓
 *   BeanFactory.refresh() 时自动实例化
 * </pre>
 * <p>
 * <b>派生注解示例：</b>
 * <pre>
 *   @Component                    ← 元注解
 *   public @interface RestController {}
 * </pre>
 *
 * @see com.mini.spring.core.ClassPathBeanDefinitionScanner 扫描器识别此注解
 * @see Configuration 配置类也是 @Component 的派生注解
 */
@Target(ElementType.TYPE)          // 只能标注在类上
@Retention(RetentionPolicy.RUNTIME) // 运行时保留，反射可访问
public @interface Component {
    /**
     * Bean 的名称
     * <p>
     * 默认为空字符串，此时 Bean 名称为类名首字母小写。
     * 例如：UserService → "userService"
     * <p>
     * 可自定义：@Component("myService") → Bean 名称为 "myService"
     */
    String value() default "";
}
