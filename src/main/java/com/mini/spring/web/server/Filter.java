package com.mini.spring.web.server;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

/**
 * 过滤器 —— 对标 Servlet 规范中的 javax.servlet.Filter
 * <p>
 * <b>学习要点：</b>
 * 过滤器是 Web 框架中最通用的请求/响应拦截机制，工作在 Servlet 容器层面。
 * 与 Spring MVC 的 HandlerInterceptor 不同，Filter 在更底层运行，
 * 可以拦截所有 HTTP 请求（包括静态资源），而不仅仅是 Controller 处理的请求。
 * <p>
 * <b>典型使用场景：</b>
 * <ul>
 *   <li>字符编码设置</li>
 *   <li>CORS 跨域处理</li>
 *   <li>请求日志记录</li>
 *   <li>身份认证（Token 校验）</li>
 *   <li>请求体压缩 / 响应缓存</li>
 * </ul>
 * <p>
 * <b>过滤器链原理：</b>
 * <pre>
 *   HTTP 请求
 *       ↓
 *   Filter1.doFilter()
 *       ↓  chain.doFilter()
 *   Filter2.doFilter()
 *       ↓  chain.doFilter()
 *   DispatcherHandler.handle()  ← 最终处理
 *       ↑
 *   Filter2 继续执行（chain.doFilter 返回后）
 *       ↑
 *   Filter1 继续执行
 *       ↓
 *   HTTP 响应
 * </pre>
 * <p>
 * 每个 Filter 调用 {@code chain.doFilter()} 将请求传递给下一个 Filter，
 * 最后一个 Filter 的 chain.doFilter() 会触发 DispatcherHandler 执行。
 * 这种「责任链」模式让多个 Filter 可以按顺序协作处理请求。
 *
 * @see FilterChain 过滤器链
 * @see HandlerInterceptor 拦截器（更高层的拦截）
 */
public interface Filter {

    /**
     * 过滤处理 —— 拦截 HTTP 请求并决定是否放行
     * <p>
     * 对标 Servlet 的 {@code Filter.doFilter()}。
     * 调用 {@code chain.doFilter()} 将请求传递给链中的下一个 Filter，
     * 不调用则请求被拦截（直接返回错误响应）。
     * <p>
     * <b>使用示例：</b>
     * <pre>
     *   public void doFilter(HttpExchange exchange, FilterChain chain) throws IOException {
     *       // 前置处理（请求到达 Controller 之前）
     *       System.out.println("请求: " + exchange.getRequestURI());
     *
     *       // 放行：传递给下一个 Filter 或最终 Handler
     *       chain.doFilter(exchange);
     *
     *       // 后置处理（Controller 处理完毕后）
     *       System.out.println("响应已发送");
     *   }
     * </pre>
     *
     * @param exchange HTTP 请求/响应对象
     * @param chain    过滤器链，调用 chain.doFilter() 放行请求
     */
    void doFilter(HttpExchange exchange, FilterChain chain) throws IOException;

    /**
     * 过滤器顺序 —— 数值越小越先执行
     * <p>
     * 对标 Spring 的 {@code @Order} 注解或 {@code Ordered} 接口。
     * 默认值为 0，可按需调整执行顺序。
     *
     * @return 顺序值（默认 0）
     */
    default int getOrder() {
        return 0;
    }
}
