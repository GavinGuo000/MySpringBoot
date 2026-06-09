package com.mini.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 后置通知（After Advice）—— 在目标方法执行后执行（无论是否抛出异常）
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring AOP 的 {@code @After} 注解（相当于 finally 块）。
 * 后置通知在目标方法执行完毕后执行，可用于资源清理、日志记录等。
 * <p>
 * <b>使用方式：</b>
 * <pre>
 *   @After("execution(* com.mini.demo.service.*.*(..))")
 *   public void logAfter(JoinPoint joinPoint) {
 *       System.out.println("方法执行后: " + joinPoint.getMethodName());
 *   }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface After {
    /**
     * 切点表达式（Pointcut Expression）
     * <p>
     * 格式同 @Before
     */
    String value();
}
