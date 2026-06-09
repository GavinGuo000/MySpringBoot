package com.mini.demo.service;

import com.mini.spring.annotation.After;
import com.mini.spring.annotation.Around;
import com.mini.spring.annotation.Aspect;
import com.mini.spring.annotation.Before;
import com.mini.spring.annotation.Component;
import com.mini.spring.aop.JoinPoint;
import com.mini.spring.aop.ProceedingJoinPoint;

import java.util.Arrays;

/**
 * 日志切面 —— 展示 AOP 动态代理的三种通知类型
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring AOP 的 AspectJ 切面。通过 @Aspect + @Component 标注，
 * IoC 容器会自动扫描并注册此切面，AOP 框架会将通知方法织入匹配的目标方法。
 * <p>
 * <b>本示例包含三种通知：</b>
 * <ul>
 *   <li>{@code @Before}  —— 方法执行前打印日志</li>
 *   <li>{@code @After}   —— 方法执行后打印日志</li>
 *   <li>{@code @Around}  —— 环绕通知，测量方法执行耗时</li>
 * </ul>
 * <p>
 * <b>切点说明：</b>
 * 本切面拦截 {@code com.mini.demo.service} 包下所有类的所有方法。
 * 调用 UserService 的任意方法时，都会触发日志输出和耗时统计。
 */
@Aspect
@Component
public class LoggingAspect {

    /**
     * 前置通知 —— 在目标方法执行前打印调用信息
     * <p>
     * 拦截 service 包下所有类的所有方法
     */
    @Before("execution(* com.mini.demo.service.*.*(..))")
    public void logBefore(JoinPoint joinPoint) {
        System.out.println("[AOP-Before] "
                + joinPoint.getTargetClass().getSimpleName() + "."
                + joinPoint.getMethodName() + "("
                + Arrays.toString(joinPoint.getArgs()) + ")");
    }

    /**
     * 后置通知 —— 在目标方法执行后打印完成信息
     */
    @After("execution(* com.mini.demo.service.*.*(..))")
    public void logAfter(JoinPoint joinPoint) {
        System.out.println("[AOP-After] "
                + joinPoint.getTargetClass().getSimpleName() + "."
                + joinPoint.getMethodName() + " 执行完毕");
    }

    /**
     * 环绕通知 —— 测量目标方法的执行耗时
     * <p>
     * 注意：@Around 通知必须调用 {@code joinPoint.proceed()} 来执行目标方法，
     * 否则目标方法不会执行。返回值会替代目标方法的原始返回值。
     */
    @Around("execution(* com.mini.demo.service.*.*(..))")
    public Object measureTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed(); // 执行目标方法
            return result;
        } finally {
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            System.out.println("[AOP-Around] "
                    + joinPoint.getTargetClass().getSimpleName() + "."
                    + joinPoint.getMethodName() + " 耗时: " + elapsed + "ms");
        }
    }
}
