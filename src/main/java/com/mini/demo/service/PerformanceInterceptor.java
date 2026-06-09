package com.mini.demo.service;

import com.mini.spring.annotation.Component;
import com.mini.spring.web.server.HandlerInterceptor;
import com.sun.net.httpserver.HttpExchange;

/**
 * 示例拦截器 —— 请求耗时统计
 * <p>
 * <b>学习要点：</b>
 * 拦截器在 Controller 方法执行前后插入自定义逻辑，
 * 与 Filter 不同，拦截器可以获取到 Controller 的方法信息。
 * <p>
 * <b>使用方式：</b>
 * 只需实现 {@link HandlerInterceptor} 接口并标注 {@code @Component}，
 * 框架会在启动时自动发现并注册此拦截器（默认拦截所有路径）。
 * <p>
 * <b>三个回调方法：</b>
 * <ul>
 *   <li>{@code preHandle} —— Controller 执行前（可用于权限校验、参数预处理）</li>
 *   <li>{@code postHandle} —— Controller 执行后（可用于修改响应头、追加返回数据）</li>
 *   <li>{@code afterCompletion} —— 请求完成后（无论成功或异常都执行，适合资源清理）</li>
 * </ul>
 *
 * @see HandlerInterceptor 拦截器接口
 */
@Component
public class PerformanceInterceptor implements HandlerInterceptor {

    /** ThreadLocal 存储请求开始时间（线程安全） */
    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    /**
     * 前置处理 —— Controller 执行之前调用
     * <p>
     * 记录请求开始时间，用于后续计算耗时。
     * 返回 true 表示继续执行，返回 false 则中断请求。
     *
     * @param exchange HTTP 请求/响应对象
     * @param handler  即将执行的 Controller 方法
     * @return true 继续执行
     */
    @Override
    public boolean preHandle(HttpExchange exchange, Object handler) throws Exception {
        START_TIME.set(System.currentTimeMillis());
        String path = exchange.getRequestURI().getPath();
        System.out.println("[PerformanceInterceptor] preHandle: " + path);
        return true; // 放行，继续执行
    }

    /**
     * 后置处理 —— Controller 执行之后调用
     * <p>
     * 此时 Controller 已执行完毕，可以获取到返回值。
     * 注意：只有 preHandle 返回 true 时才会执行。
     *
     * @param exchange   HTTP 请求/响应对象
     * @param handler    Controller 方法
     * @param controller Controller 方法的返回值
     */
    @Override
    public void postHandle(HttpExchange exchange, Object handler, Object controller) throws Exception {
        System.out.println("[PerformanceInterceptor] postHandle: Controller 返回结果 = "
                + (controller != null ? controller.getClass().getSimpleName() : "null"));
    }

    /**
     * 完成后回调 —— 请求处理完毕后调用（无论成功或异常）
     * <p>
     * 计算并打印请求耗时，适合用于性能监控。
     * 即使 Controller 抛出异常，此方法也会被调用。
     *
     * @param exchange HTTP 请求/响应对象
     * @param handler  Controller 方法
     * @param ex       处理过程中抛出的异常（可能为 null）
     */
    @Override
    public void afterCompletion(HttpExchange exchange, Object handler, Exception ex) throws Exception {
        Long startTime = START_TIME.get();
        if (startTime != null) {
            long elapsed = System.currentTimeMillis() - startTime;
            String path = exchange.getRequestURI().getPath();
            System.out.println("[PerformanceInterceptor] afterCompletion: " + path
                    + " 耗时 " + elapsed + "ms"
                    + (ex != null ? " [异常: " + ex.getMessage() + "]" : ""));
            START_TIME.remove(); // 清理 ThreadLocal，避免内存泄漏
        }
    }
}
