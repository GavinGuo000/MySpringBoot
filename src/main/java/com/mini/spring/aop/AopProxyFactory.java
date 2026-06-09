package com.mini.spring.aop;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * AOP 代理工厂 —— 为 Bean 创建代理对象，支持两种代理策略
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring AOP 的 {@code DefaultAopProxyFactory}，它会根据条件选择：
 * <ul>
 *   <li><b>JDK 动态代理</b>（{@code JdkDynamicAopProxy}）—— 目标类实现了接口时使用</li>
 *   <li><b>CGLIB 代理</b>（{@code CglibAopProxy}）—— 目标类没有实现接口时，通过字节码生成子类代理</li>
 * </ul>
 * <p>
 * <b>JDK 动态代理原理：</b>
 * <pre>
 *   原始 Bean（UserService）
 *       ↓ Proxy.newProxyInstance()
 *   代理对象（Proxy）
 *       实现了目标 Bean 的所有接口
 *       所有方法调用都被转发到 InvocationHandler.invoke()
 *       在 invoke() 中织入 Before/After/Around 通知
 * </pre>
 * <p>
 * <b>CGLIB 代理原理（ByteBuddy 实现）：</b>
 * <pre>
 *   原始 Bean（GreetingService）
 *       ↓ ByteBuddy.subclass()
 *   代理对象（GreetingService$$ByteBuddy$$xxx）
 *       是目标类的子类
 *       重写了目标类的所有非 final 方法
 *       方法调用被转发到 CglibInterceptor.intercept()
 *       在 intercept() 中织入 Before/After/Around 通知
 * </pre>
 */
public class AopProxyFactory {

    private final AspectManager aspectManager;

    public AopProxyFactory(AspectManager aspectManager) {
        this.aspectManager = aspectManager;
    }

    /**
     * 为目标 Bean 创建 AOP 代理
     * <p>
     * 自动选择代理策略：
     * <ul>
     *   <li>目标类实现了接口 → 使用 JDK 动态代理（{@code Proxy.newProxyInstance}）</li>
     *   <li>目标类没有接口 → 使用 CGLIB 代理（ByteBuddy 生成子类）</li>
     * </ul>
     * <p>
     * 代理对象的方法调用流程：
     * <pre>
     *   调用 proxy.getUserById(1)
     *       ↓
     *   InvocationHandler.invoke(proxy, method, args)   ← JDK 代理
     *   或 CglibInterceptor.intercept(method, args, superCall) ← CGLIB 代理
     *       ↓
     *   1. 检查是否有匹配的通知
     *       ↓ 有
     *   2. 执行 @Before 通知
     *       ↓
     *   3. 执行 @Around 通知（内部调用 proceed()）
     *       ↓ 或
     *   3. 直接调用目标方法
     *       ↓
     *   4. 执行 @After 通知
     *       ↓
     *   5. 返回结果
     * </pre>
     *
     * @param target      目标 Bean 实例
     * @param targetClass 目标 Bean 的类
     * @return 代理对象
     */
    public Object createProxy(Object target, Class<?> targetClass) {
        // 获取目标类实现的所有接口
        Class<?>[] interfaces = targetClass.getInterfaces();

        if (interfaces.length > 0) {
            // === 策略 1：JDK 动态代理（目标类有接口） ===
            System.out.println("[MiniSpring-AOP] 为 " + targetClass.getSimpleName()
                    + " 创建 JDK 动态代理，接口: " + Arrays.toString(interfaces));
            return Proxy.newProxyInstance(
                    targetClass.getClassLoader(),
                    interfaces,
                    new AopInvocationHandler(target, targetClass)
            );
        } else {
            // === 策略 2：CGLIB 代理（目标类无接口，使用 ByteBuddy 生成子类） ===
            System.out.println("[MiniSpring-AOP] 为 " + targetClass.getSimpleName()
                    + " 创建 CGLIB 代理（ByteBuddy 子类代理）");
            return createCglibProxy(target, targetClass);
        }
    }

    /**
     * 使用 ByteBuddy 创建 CGLIB 风格的子类代理
     * <p>
     * 对标 Spring 的 {@code CglibAopProxy}。
     * 原理：生成目标类的子类，重写所有非 final 方法，在方法中织入 AOP 通知。
     * <p>
     * <b>CGLIB 与 JDK 代理的区别：</b>
     * <ul>
     *   <li>JDK 代理：生成实现目标接口的代理类（横向，基于接口）</li>
     *   <li>CGLIB 代理：生成目标类的子类（纵向，基于继承）</li>
     * </ul>
     *
     * @param target      目标 Bean 实例
     * @param targetClass 目标 Bean 的类
     * @return 代理对象（目标类的子类实例）
     */
    private Object createCglibProxy(Object target, Class<?> targetClass) {
        try {
            return new ByteBuddy()
                    // 以目标类为父类，生成子类
                    .subclass(targetClass)
                    // 拦截所有非 Object 类声明的方法（即业务方法）
                    .method(ElementMatchers.not(
                            ElementMatchers.isDeclaredBy(Object.class)))
                    // 将方法调用转发给 CglibInterceptor
                    .intercept(MethodDelegation.to(
                            new CglibInterceptor(target, targetClass)))
                    // 创建 Class 并实例化
                    .make()
                    .load(targetClass.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded()
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            System.err.println("[MiniSpring-AOP] 警告: 无法为 " + targetClass.getSimpleName()
                    + " 创建 CGLIB 代理（可能需要无参构造器）: " + e.getMessage());
            return target;
        }
    }

    /**
     * AOP 调用处理器 —— 所有代理方法的调用都经过这里
     * <p>
     * 这是 JDK 动态代理的核心：{@code InvocationHandler}。
     * 对标 Spring 的 {@code JdkDynamicAopProxy.invoke()} 方法。
     */
    private class AopInvocationHandler implements InvocationHandler {

        private final Object target;
        private final Class<?> targetClass;

        AopInvocationHandler(Object target, Class<?> targetClass) {
            this.target = target;
            this.targetClass = targetClass;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 获取匹配当前方法的所有通知
            List<AspectManager.AdviceInfo> advices =
                    aspectManager.getMatchingAdvices(targetClass, method.getName());

            // 没有匹配的通知，直接调用目标方法（无代理开销）
            if (advices.isEmpty()) {
                return method.invoke(target, args);
            }

            // === 有通知需要织入 ===

            // 创建 JoinPoint 实例（封装方法调用上下文）
            JoinPointImpl joinPoint = new JoinPointImpl(target, targetClass, method, args);
            Object result = null;

            try {
                // 第 1 步：执行所有 @Before 通知
                for (AspectManager.AdviceInfo advice : advices) {
                    if (advice.getType() == AspectManager.AdviceInfo.Type.BEFORE) {
                        invokeAdvice(advice, joinPoint);
                    }
                }

                // 第 2 步：执行目标方法（可能被 @Around 包裹）
                AspectManager.AdviceInfo aroundAdvice = findAroundAdvice(advices);
                if (aroundAdvice != null) {
                    // 有 @Around 通知：创建 ProceedingJoinPoint，由 Around 通知控制目标方法执行
                    ProceedingJoinPointImpl proceedingJoinPoint =
                            new ProceedingJoinPointImpl(target, targetClass, method, args);
                    result = invokeAroundAdvice(aroundAdvice, proceedingJoinPoint);
                } else {
                    // 无 @Around：直接调用目标方法
                    result = method.invoke(target, args);
                }
            } finally {
                // 第 3 步：执行所有 @After 通知（finally 确保异常时也能执行）
                for (AspectManager.AdviceInfo advice : advices) {
                    if (advice.getType() == AspectManager.AdviceInfo.Type.AFTER) {
                        invokeAdvice(advice, joinPoint);
                    }
                }
            }

            return result;
        }

        /**
         * 调用普通通知方法（@Before / @After）
         */
        private void invokeAdvice(AspectManager.AdviceInfo advice, JoinPointImpl joinPoint) throws Throwable {
            Method adviceMethod = advice.getAdviceMethod();
            adviceMethod.setAccessible(true);
            Class<?>[] paramTypes = adviceMethod.getParameterTypes();
            if (paramTypes.length > 0 && JoinPoint.class.isAssignableFrom(paramTypes[0])) {
                adviceMethod.invoke(advice.getAspectBean(), joinPoint);
            } else {
                adviceMethod.invoke(advice.getAspectBean());
            }
        }

        /**
         * 调用环绕通知方法（@Around）
         */
        private Object invokeAroundAdvice(AspectManager.AdviceInfo advice,
                                          ProceedingJoinPointImpl joinPoint) throws Throwable {
            Method adviceMethod = advice.getAdviceMethod();
            adviceMethod.setAccessible(true);
            return adviceMethod.invoke(advice.getAspectBean(), joinPoint);
        }

        /**
         * 查找第一个 @Around 通知
         */
        private AspectManager.AdviceInfo findAroundAdvice(List<AspectManager.AdviceInfo> advices) {
            for (AspectManager.AdviceInfo advice : advices) {
                if (advice.getType() == AspectManager.AdviceInfo.Type.AROUND) {
                    return advice;
                }
            }
            return null;
        }
    }

    // ======================== CGLIB 风格拦截器（ByteBuddy） ========================

    /**
     * CGLIB 风格的拦截器 —— 用于 ByteBuddy 子类代理
     * <p>
     * 对标 Spring 的 {@code CglibAopProxy.DynamicAdvisedInterceptor}。
     * ByteBuddy 通过 {@code MethodDelegation} 将目标类的方法调用转发到此对象。
     * <p>
     * <b>与 JDK InvocationHandler 的对比：</b>
     * <ul>
     *   <li>JDK：{@code invoke(Object proxy, Method method, Object[] args)}</li>
     *   <li>CGLIB：{@code intercept(@Origin Method, @AllArguments Object[], @SuperCall Callable)}
     *       —— {@code @SuperCall} 相当于调用父类（即目标类）的原始方法</li>
     * </ul>
     */
    public class CglibInterceptor {

        private final Object target;
        private final Class<?> targetClass;

        CglibInterceptor(Object target, Class<?> targetClass) {
            this.target = target;
            this.targetClass = targetClass;
        }

        /**
         * ByteBuddy 拦截入口 —— 所有被代理的方法调用都经过这里
         * <p>
         * {@code @RuntimeType}    允许返回 Object 类型（ByteBuddy 自动转换）<br>
         * {@code @Origin}         注入被调用的原始 Method 对象<br>
         * {@code @AllArguments}   注入方法的所有参数<br>
         * {@code @SuperCall}      注入父类方法的 Callable（调用即执行原始方法）
         */
        @RuntimeType
        public Object intercept(@Origin Method method,
                                @AllArguments Object[] args,
                                @SuperCall Callable<?> superCall) throws Throwable {
            // 获取匹配当前方法的所有通知
            List<AspectManager.AdviceInfo> advices =
                    aspectManager.getMatchingAdvices(targetClass, method.getName());

            // 没有匹配的通知，直接调用父类方法（无代理开销）
            if (advices.isEmpty()) {
                return superCall.call();
            }

            // === 有通知需要织入 ===
            JoinPointImpl joinPoint = new JoinPointImpl(target, targetClass, method, args);
            Object result = null;

            try {
                // 第 1 步：执行所有 @Before 通知
                for (AspectManager.AdviceInfo advice : advices) {
                    if (advice.getType() == AspectManager.AdviceInfo.Type.BEFORE) {
                        invokeAdvice(advice, joinPoint);
                    }
                }

                // 第 2 步：执行目标方法（可能被 @Around 包裹）
                AspectManager.AdviceInfo aroundAdvice = findAroundAdvice(advices);
                if (aroundAdvice != null) {
                    // 有 @Around 通知：创建 ProceedingJoinPoint，由 Around 通知控制目标方法执行
                    ProceedingJoinPointImpl proceedingJoinPoint =
                            new ProceedingJoinPointImpl(target, targetClass, method, args);
                    result = invokeAroundAdvice(aroundAdvice, proceedingJoinPoint);
                } else {
                    // 无 @Around：调用父类方法（即目标类的原始方法）
                    result = superCall.call();
                }
            } finally {
                // 第 3 步：执行所有 @After 通知（finally 确保异常时也能执行）
                for (AspectManager.AdviceInfo advice : advices) {
                    if (advice.getType() == AspectManager.AdviceInfo.Type.AFTER) {
                        invokeAdvice(advice, joinPoint);
                    }
                }
            }

            return result;
        }

        private void invokeAdvice(AspectManager.AdviceInfo advice, JoinPointImpl joinPoint) throws Throwable {
            Method adviceMethod = advice.getAdviceMethod();
            adviceMethod.setAccessible(true);
            Class<?>[] paramTypes = adviceMethod.getParameterTypes();
            if (paramTypes.length > 0 && JoinPoint.class.isAssignableFrom(paramTypes[0])) {
                adviceMethod.invoke(advice.getAspectBean(), joinPoint);
            } else {
                adviceMethod.invoke(advice.getAspectBean());
            }
        }

        private Object invokeAroundAdvice(AspectManager.AdviceInfo advice,
                                          ProceedingJoinPointImpl joinPoint) throws Throwable {
            Method adviceMethod = advice.getAdviceMethod();
            adviceMethod.setAccessible(true);
            return adviceMethod.invoke(advice.getAspectBean(), joinPoint);
        }

        private AspectManager.AdviceInfo findAroundAdvice(List<AspectManager.AdviceInfo> advices) {
            for (AspectManager.AdviceInfo advice : advices) {
                if (advice.getType() == AspectManager.AdviceInfo.Type.AROUND) {
                    return advice;
                }
            }
            return null;
        }
    }

    // ======================== JoinPoint 实现类 ========================

    /**
     * JoinPoint 的默认实现 —— 用于 @Before 和 @After 通知
     */
    static class JoinPointImpl implements JoinPoint {
        private final Object target;
        private final Class<?> targetClass;
        private final Method method;
        private final Object[] args;

        JoinPointImpl(Object target, Class<?> targetClass, Method method, Object[] args) {
            this.target = target;
            this.targetClass = targetClass;
            this.method = method;
            this.args = args != null ? args : new Object[0];
        }

        @Override public Object getTarget() { return target; }
        @Override public Class<?> getTargetClass() { return targetClass; }
        @Override public Method getMethod() { return method; }
        @Override public String getMethodName() { return method.getName(); }
        @Override public Object[] getArgs() { return args; }
    }

    /**
     * ProceedingJoinPoint 的实现 —— 用于 @Around 通知
     * <p>
     * 关键：{@link #proceed()} 方法会调用目标对象的原始方法
     */
    static class ProceedingJoinPointImpl implements ProceedingJoinPoint {
        private final Object target;
        private final Class<?> targetClass;
        private final Method method;
        private final Object[] args;

        ProceedingJoinPointImpl(Object target, Class<?> targetClass, Method method, Object[] args) {
            this.target = target;
            this.targetClass = targetClass;
            this.method = method;
            this.args = args != null ? args : new Object[0];
        }

        @Override public Object getTarget() { return target; }
        @Override public Class<?> getTargetClass() { return targetClass; }
        @Override public Method getMethod() { return method; }
        @Override public String getMethodName() { return method.getName(); }
        @Override public Object[] getArgs() { return args; }

        /**
         * 执行目标方法 —— 这是 @Around 通知的核心
         * <p>
         * 调用目标对象上的原始方法，并返回其结果。
         */
        @Override
        public Object proceed() throws Throwable {
            return method.invoke(target, args);
        }
    }
}
