package com.mini.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于 @Configuration 类中的方法，声明该方法返回一个 Bean 交由容器管理
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring 的 {@code org.springframework.context.annotation.Bean}。
 * 这是用 Java 代码定义 Bean 的方式，替代 XML 配置。
 * <p>
 * <b>使用示例：</b>
 * <pre>
 *   @Configuration
 *   public class AppConfig {
 *
 *       @Bean                             // Bean 名称默认为方法名 "greetingService"
 *       public GreetingService greetingService() {
 *           return new GreetingService();
 *       }
 *
 *       @Bean("customUserRepo")           // 自定义 Bean 名称
 *       public UserRepository userRepository() {
 *           return new UserRepository();
 *       }
 *   }
 * </pre>
 * <p>
 * <b>处理流程：</b>
 * BeanFactory.processBeanMethods() 会扫描 @Configuration 类中的 @Bean 方法，
 * 将方法信息封装为 BeanDefinition（包含 factoryBeanName 和 factoryMethod），
 * 在实例化阶段通过反射调用该方法创建 Bean。
 *
 * @see Configuration 配置类注解
 * @see com.mini.spring.core.BeanFactory#processBeanMethods() 处理 @Bean 方法
 */
@Target(ElementType.METHOD)         // 只能标注在方法上
@Retention(RetentionPolicy.RUNTIME)  // 运行时保留
public @interface Bean {
    /**
     * Bean 名称
     * <p>
     * 默认为空字符串，此时 Bean 名称为方法名。
     * 例如：greetingService() → "greetingService"
     */
    String value() default "";
}
