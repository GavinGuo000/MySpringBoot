package com.mini.spring.aop;

import com.mini.spring.annotation.After;
import com.mini.spring.annotation.Around;
import com.mini.spring.annotation.Aspect;
import com.mini.spring.annotation.Before;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 切面管理器 —— 解析 @Aspect 切面 Bean，匹配目标方法
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring AOP 的 {@code AspectJAdvisorFactory} + {@code DefaultPointcutAdvisor}。
 * <p>
 * <b>核心职责：</b>
 * <ol>
 *   <li>扫描所有 @Aspect 切面 Bean，提取 @Before/@After/@Around 通知方法</li>
 *   <li>解析切点表达式（Pointcut），将其转为可匹配的正则模式</li>
 *   <li>判断目标 Bean 的某个方法是否匹配某条通知</li>
 * </ol>
 * <p>
 * <b>切点表达式格式（简化版 execution）：</b>
 * <pre>
 *   execution(* com.mini.demo.service.*.*(..))
 *              ↑ ────────────────────────── ── ──
 *              返回值    包.类              方法 参数
 *
 *   通配符说明：
 *   *   → 匹配任意（类名/方法名/返回值）
 *   ..  → 匹配任意参数列表
 * </pre>
 */
public class AspectManager {

    /**
     * 通知信息 —— 描述一条 AOP 通知（Advice）
     * <p>
     * 包含：通知类型、切面 Bean 实例、通知方法、切点模式
     */
    public static class AdviceInfo {
        /** 通知类型 */
        public enum Type { BEFORE, AFTER, AROUND }

        private final Type type;
        private final Object aspectBean;
        private final Method adviceMethod;
        private final String pointcutPattern;

        public AdviceInfo(Type type, Object aspectBean, Method adviceMethod, String pointcutPattern) {
            this.type = type;
            this.aspectBean = aspectBean;
            this.adviceMethod = adviceMethod;
            this.pointcutPattern = pointcutPattern;
        }

        public Type getType() { return type; }
        public Object getAspectBean() { return aspectBean; }
        public Method getAdviceMethod() { return adviceMethod; }
        public String getPointcutPattern() { return pointcutPattern; }
    }

    /** 所有已解析的通知列表 */
    private final List<AdviceInfo> advices = new ArrayList<>();

    /**
     * 从切面 Bean 中解析通知
     * <p>
     * 扫描 @Aspect 类中标注了 @Before/@After/@Around 的方法，
     * 将它们注册为 AdviceInfo。
     *
     * @param aspectBean 切面 Bean 实例（已标注 @Aspect）
     */
    public void addAspect(Object aspectBean) {
        Class<?> clazz = aspectBean.getClass();
        if (!clazz.isAnnotationPresent(Aspect.class)) {
            return;
        }

        System.out.println("[MiniSpring-AOP] 解析切面: " + clazz.getSimpleName());

        for (Method method : clazz.getDeclaredMethods()) {
            // @Before 通知
            Before before = method.getAnnotation(Before.class);
            if (before != null) {
                advices.add(new AdviceInfo(
                        AdviceInfo.Type.BEFORE, aspectBean, method, before.value()));
                System.out.println("[MiniSpring-AOP]   注册 @Before: " + method.getName()
                        + " -> " + before.value());
            }

            // @After 通知
            After after = method.getAnnotation(After.class);
            if (after != null) {
                advices.add(new AdviceInfo(
                        AdviceInfo.Type.AFTER, aspectBean, method, after.value()));
                System.out.println("[MiniSpring-AOP]   注册 @After: " + method.getName()
                        + " -> " + after.value());
            }

            // @Around 通知
            Around around = method.getAnnotation(Around.class);
            if (around != null) {
                advices.add(new AdviceInfo(
                        AdviceInfo.Type.AROUND, aspectBean, method, around.value()));
                System.out.println("[MiniSpring-AOP]   注册 @Around: " + method.getName()
                        + " -> " + around.value());
            }
        }
    }

    /**
     * 获取匹配目标方法的所有通知
     *
     * @param targetClass 目标类（实现接口的那个类）
     * @param methodName  目标方法名
     * @return 匹配的通知列表（可能为空）
     */
    public List<AdviceInfo> getMatchingAdvices(Class<?> targetClass, String methodName) {
        List<AdviceInfo> matched = new ArrayList<>();
        for (AdviceInfo advice : advices) {
            if (matchesPointcut(advice.getPointcutPattern(), targetClass, methodName)) {
                matched.add(advice);
            }
        }
        return matched;
    }

    /**
     * 判断目标 Bean 是否需要被代理（是否有任何通知匹配它的任意方法）
     *
     * @param targetClass 目标类
     * @return true 如果该类需要 AOP 代理
     */
    public boolean needsProxy(Class<?> targetClass) {
        for (Method method : targetClass.getMethods()) {
            if (method.getDeclaringClass() == Object.class) continue;
            for (AdviceInfo advice : advices) {
                if (matchesPointcut(advice.getPointcutPattern(), targetClass, method.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 切点表达式匹配
     * <p>
     * 解析 execution(* com.xxx.Class.method(..)) 格式的表达式，
     * 判断是否匹配给定的类和方法。
     * <p>
     * 匹配规则：
     * <ol>
     *   <li>提取 "类名模式.方法名模式" 部分</li>
     *   <li>将 * 转为 [^.]+（匹配单个段），.. 转为 .*（匹配任意）</li>
     *   <li>用正则匹配 "全限定类名.方法名"</li>
     * </ol>
     *
     * @param pointcut 切点表达式
     * @param targetClass 目标类
     * @param methodName 方法名
     * @return true 如果匹配
     */
    private boolean matchesPointcut(String pointcut, Class<?> targetClass, String methodName) {
        try {
            // 从 "execution(...)" 中提取括号内的内容
            String expr = pointcut.trim();
            if (expr.startsWith("execution(") && expr.endsWith(")")) {
                expr = expr.substring("execution(".length(), expr.length() - 1).trim();
            }

            // 跳过返回值类型（第一个 * 或类型名）
            // 格式: returnType package.Class.method(params)
            int firstSpace = expr.indexOf(' ');
            if (firstSpace > 0) {
                expr = expr.substring(firstSpace + 1).trim();
            }

            // 去掉参数部分 "(..)"
            int parenIdx = expr.indexOf('(');
            if (parenIdx > 0) {
                expr = expr.substring(0, parenIdx);
            }

            // 现在 expr = "com.mini.demo.service.*.*"
            // 将通配符转为正则：
            //   .. → __DOTS__（临时占位，避免被 * 规则覆盖）
            //   * → [^.]+（匹配单个路径段）
            //   __DOTS__ → .*（匹配任意路径）
            String regex = expr
                    .replace("..", "__DOTS__")
                    .replace("*", "[^.]+")
                    .replace("__DOTS__", ".*");

            // 构建待匹配的字符串：完整类名.方法名
            String fullName = targetClass.getName() + "." + methodName;

            return fullName.matches(regex);
        } catch (Exception e) {
            System.err.println("[MiniSpring-AOP] 切点表达式匹配失败: " + pointcut + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * 是否已注册任何切面
     */
    public boolean hasAspects() {
        return !advices.isEmpty();
    }
}
