package com.mini.spring.aop;

import java.lang.reflect.Method;

/**
 * 连接点（JoinPoint）—— 表示目标方法的执行上下文
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring AOP 的 {@code org.aspectj.lang.JoinPoint}。
 * 连接点封装了被拦截方法的所有信息，供通知（Advice）使用。
 * <p>
 * <b>在 @Before 和 @After 中使用：</b>
 * <pre>
 *   @Before("execution(* com.mini.demo.service.*.*(..))")
 *   public void logBefore(JoinPoint joinPoint) {
 *       System.out.println("类名: " + joinPoint.getTargetClass().getSimpleName());
 *       System.out.println("方法名: " + joinPoint.getMethodName());
 *       System.out.println("参数: " + Arrays.toString(joinPoint.getArgs()));
 *   }
 * </pre>
 *
 * @see ProceedingJoinPoint 环绕通知使用此子接口
 */
public interface JoinPoint {

    /**
     * 获取目标对象（被代理的原始对象）
     */
    Object getTarget();

    /**
     * 获取目标对象的类
     */
    Class<?> getTargetClass();

    /**
     * 获取被拦截的方法对象
     */
    Method getMethod();

    /**
     * 获取方法名
     */
    String getMethodName();

    /**
     * 获取方法的参数数组
     */
    Object[] getArgs();
}
