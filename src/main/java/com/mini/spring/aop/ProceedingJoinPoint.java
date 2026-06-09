package com.mini.spring.aop;

/**
 * 可执行的连接点 —— 环绕通知（@Around）专用
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring AOP 的 {@code org.aspectj.lang.ProceedingJoinPoint}。
 * 继承 JoinPoint，增加了 {@link #proceed()} 方法，
 * 允许通知控制目标方法是否执行。
 * <p>
 * <b>在 @Around 中使用：</b>
 * <pre>
 *   @Around("execution(* com.mini.demo.service.*.*(..))")
 *   public Object measureTime(ProceedingJoinPoint joinPoint) throws Throwable {
 *       long start = System.currentTimeMillis();
 *       Object result = joinPoint.proceed();   // ← 调用目标方法
 *       long elapsed = System.currentTimeMillis() - start;
 *       System.out.println("耗时: " + elapsed + "ms");
 *       return result;
 *   }
 * </pre>
 * <p>
 * <b>重要：</b>
 * 如果不调用 {@code proceed()}，目标方法将不会执行！
 * 这在实现权限控制、缓存等场景时非常有用。
 *
 * @see JoinPoint 基础连接点接口
 */
public interface ProceedingJoinPoint extends JoinPoint {

    /**
     * 执行目标方法
     * <p>
     * 调用此方法会继续执行被拦截的目标方法，
     * 并返回目标方法的返回值。
     *
     * @return 目标方法的返回值
     * @throws Throwable 目标方法抛出的异常
     */
    Object proceed() throws Throwable;
}
