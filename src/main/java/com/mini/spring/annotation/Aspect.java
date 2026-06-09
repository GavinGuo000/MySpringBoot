package com.mini.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个类为 AOP 切面（Aspect）
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring 的 {@code org.springframework.stereotype.Component} + AOP 切面。
 * 切面类中定义的通知（Advice）方法会在目标方法执行前后织入。
 * <p>
 * <b>使用方式：</b>
 * <pre>
 *   @Aspect
 *   @Component
 *   public class LoggingAspect {
 *       @Before("execution(* com.mini.demo.service.*.*(..))")
 *       public void logBefore(JoinPoint joinPoint) { ... }
 *   }
 * </pre>
 * <p>
 * 注意：切面类需要同时标注 @Component，才能被 IoC 容器扫描和管理。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Aspect {
    /**
     * 切面的名称（可选），默认使用类名
     */
    String value() default "";
}
