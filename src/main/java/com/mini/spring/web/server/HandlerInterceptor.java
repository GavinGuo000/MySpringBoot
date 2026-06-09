package com.mini.spring.web.server;

import com.sun.net.httpserver.HttpExchange;

/**
 * 处理器拦截器 —— 对标 Spring MVC 的 HandlerInterceptor
 * <p>
 * <b>学习要点：</b>
 * 在 Spring MVC 中，拦截器可以在请求到达 Controller 前后执行自定义逻辑，
 * 常用于：日志记录、权限校验、请求耗时统计等场景。
 * <p>
 * <b>执行时机：</b>
 * <pre>
 *   HTTP 请求到达
 *       ↓
 *   Filter.doFilter()           ← 过滤器先执行（外层）
 *       ↓
 *   HandlerInterceptor.preHandle()   ← 拦截器前置（内层，Controller 之前）
 *       ↓
 *   Controller 方法执行
 *       ↓
 *   HandlerInterceptor.postHandle()  ← 拦截器后置（Controller 之后，响应之前）
 *       ↓
 *   HandlerInterceptor.afterCompletion() ← 完成后回调（无论成功或异常都会执行）
 *       ↓
 *   Filter 继续执行（链条下一个）
 * </pre>
 * <p>
 * <b>与 Filter 的区别：</b>
 * <ul>
 *   <li><b>拦截器</b> —— 在 Spring MVC 层工作，可以获取 Handler 信息（Controller 方法）</li>
 *   <li><b>过滤器</b> —— 在 Servlet 层工作，只能获取请求/响应，无法获取 Handler</li>
 * </ul>
 *
 * @see Filter 过滤器
 * @see DispatcherHandler 调用拦截器的核心逻辑
 */
public interface HandlerInterceptor {

    /**
     * 前置处理 —— Controller 方法执行之前调用
     * <p>
     * 对标 Spring MVC 的 {@code HandlerInterceptor.preHandle()}。
     * 常用于：权限校验、登录检查、请求参数预处理等。
     * <p>
     * <b>返回值含义：</b>
     * <ul>
     *   <li>{@code true} —— 继续执行后续拦截器和 Controller</li>
     *   <li>{@code false} —— 中断请求，不再执行后续拦截器和 Controller</li>
     * </ul>
     *
     * @param exchange HTTP 请求/响应对象
     * @param handler  即将执行的 Controller 方法信息（可为 null）
     * @return true 继续执行，false 中断请求
     */
    default boolean preHandle(HttpExchange exchange, Object handler) throws Exception {
        return true;
    }

    /**
     * 后置处理 —— Controller 方法执行之后、响应发送之前调用
     * <p>
     * 对标 Spring MVC 的 {@code HandlerInterceptor.postHandle()}。
     * 常用于：修改响应头、添加公共响应数据等。
     * <p>
     * 注意：只有 preHandle 返回 true 时才会执行此方法。
     *
     * @param exchange   HTTP 请求/响应对象
     * @param handler    已执行的 Controller 方法信息
     * @param controller Controller 方法返回的结果
     */
    default void postHandle(HttpExchange exchange, Object handler, Object controller) throws Exception {
    }

    /**
     * 完成后回调 —— 请求处理完成后调用（无论成功或异常）
     * <p>
     * 对标 Spring MVC 的 {@code HandlerInterceptor.afterCompletion()}。
     * 常用于：资源清理、请求耗时统计、日志记录等。
     * <p>
     * 注意：只有 preHandle 返回 true 时才会执行此方法，
     * 即使 Controller 抛出异常，此方法也会被调用。
     *
     * @param exchange HTTP 请求/响应对象
     * @param handler  已执行的 Controller 方法信息
     * @param ex       处理过程中抛出的异常（可能为 null）
     */
    default void afterCompletion(HttpExchange exchange, Object handler, Exception ex) throws Exception {
    }
}
