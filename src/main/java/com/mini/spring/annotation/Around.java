package com.mini.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 环绕通知（Around Advice）—— 包裹目标方法，可控制其执行
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring AOP 的 {@code @Around} 注解。
 * 环绕通知是最强大的通知类型，它可以：
 * <ul>
 *   <li>在目标方法执行前后分别执行自定义逻辑</li>
 *   <li>决定是否调用目标方法（通过 {@code joinPoint.proceed()}）</li>
 *   <li>修改目标方法的返回值或异常</li>
 * </ul>
 * <p>
 * <b>使用方式：</b>
 * <pre>
 *   @Around("execution(* com.mini.demo.service.*.*(..))")
 *   public Object measureTime(ProceedingJoinPoint joinPoint) throws Throwable {
 *       long start = System.currentTimeMillis();
 *       Object result = joinPoint.proceed();  // 调用目标方法
 *       long elapsed = System.currentTimeMillis() - start;
 *       System.out.println("耗时: " + elapsed + "ms");
 *       return result;
 *   }
 * </pre>
 * <p>
 * <b>注意：</b>
 * <ul>
 *   <li>环绕通知方法的参数类型必须是 {@code ProceedingJoinPoint}</li>
 *   <li>必须调用 {@code joinPoint.proceed()} 来执行目标方法</li>
 *   <li>返回值会替代目标方法的返回值</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Around {
    /**
     * 切点表达式（Pointcut Expression）
     * <p>
     * 格式同 @Before
     */
    String value();
}
