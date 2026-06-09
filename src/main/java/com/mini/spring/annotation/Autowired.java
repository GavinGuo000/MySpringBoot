package com.mini.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动注入依赖，支持字段注入和构造器注入
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring 的 {@code org.springframework.beans.factory.annotation.Autowired}。
 * 这是 Spring 依赖注入（DI）的核心注解。
 * <p>
 * <b>两种注入方式：</b>
 * <pre>
 *   // 方式 1：字段注入（简单但不够灵活）
 *   @Component
 *   public class UserService {
 *       @Autowired
 *       private UserRepository userRepository;  ← BeanFactory 自动注入
 *   }
 *
 *   // 方式 2：构造器注入（推荐，支持不可变性）
 *   @Component
 *   public class UserService {
 *       private final UserRepository userRepository;
 *
 *       @Autowired
 *       public UserService(UserRepository userRepository) {
 *           this.userRepository = userRepository;
 *       }
 *   }
 * </pre>
 * <p>
 * <b>注入原理：</b>
 * BeanFactory.populateBean() 会扫描所有字段/构造器，
 * 发现 @Autowired 后，按类型从容器中查找匹配的 Bean 并赋值。
 *
 * @see com.mini.spring.core.BeanFactory 在 populateBean() 中处理此注解
 */
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR}) // 可用于字段和构造器
@Retention(RetentionPolicy.RUNTIME)                    // 运行时保留
public @interface Autowired {
}
