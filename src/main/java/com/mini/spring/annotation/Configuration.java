package com.mini.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个类为配置类，相当于 @Component + 可以用 @Bean 定义 Bean
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring 的 {@code org.springframework.context.annotation.Configuration}。
 * 配置类是 Spring 中用 Java 代码代替 XML 配置的方式。
 * <p>
 * <b>注意：</b>此注解上标注了 @Component，因此它是一个派生注解。
 * ClassPathBeanDefinitionScanner 会自动识别它（通过派生注解机制）。
 * <p>
 * <b>使用示例：</b>
 * <pre>
 *   @Configuration
 *   public class AppConfig {
 *
 *       @Bean("greetingService")        // 定义一个 Bean
 *       public GreetingService greetingService() {
 *           return new GreetingService();
 *       }
 *
 *       @Bean("userService")            // 定义另一个 Bean
 *       public UserService userService(UserRepository repo) {
 *           return new UserService(repo);  // 参数会自动从容器查找
 *       }
 *   }
 * </pre>
 * <p>
 * <b>与 @Component 的区别：</b>
 * @Configuration 类中的 @Bean 方法会被 BeanFactory.processBeanMethods() 额外处理，
 * 用于注册更多的 Bean。而普通 @Component 类不会。
 *
 * @see Bean 用于配置类的方法上，定义 Bean
 * @see Component 元注解
 */
@Target(ElementType.TYPE)           // 只能标注在类上
@Retention(RetentionPolicy.RUNTIME)  // 运行时保留
@Component  // 配置类本身也是组件（派生注解）
public @interface Configuration {
    /**
     * Bean 名称（默认为类名首字母小写）
     */
    String value() default "";
}
