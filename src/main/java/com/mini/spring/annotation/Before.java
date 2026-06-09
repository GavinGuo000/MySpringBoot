package com.mini.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 前置通知（Before Advice）—— 在目标方法执行前执行
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring AOP 的 {@code @Before} 注解。
 * 前置通知在目标方法调用之前执行，可用于日志记录、权限校验等。
 * <p>
 * <b>使用方式：</b>
 * <pre>
 *   @Before("execution(* com.mini.demo.service.*.*(..))")
 *   public void logBefore(JoinPoint joinPoint) {
 *       System.out.println("方法执行前: " + joinPoint.getMethodName());
 *   }
 * </pre>
 * <p>
 * <b>切点表达式说明：</b>
 * <ul>
 *   <li>{@code execution(* com.mini.demo.service.*.*(..))} — 匹配 service 包下所有类的所有方法</li>
 *   <li>{@code execution(* com.mini.demo.service.UserService.*(..))} — 只匹配 UserService 的所有方法</li>
 *   <li>{@code *(..)} — 匹配任意方法名、任意参数</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Before {
    /**
     * 切点表达式（Pointcut Expression）
     * <p>
     * 格式：{@code execution(<返回类型> <包.类>.<方法>(<参数>))}
     * 通配符：{@code *} 表示任意，{@code ..} 表示任意参数
     */
    String value();
}
