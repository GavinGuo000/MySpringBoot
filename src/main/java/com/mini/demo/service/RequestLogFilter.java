package com.mini.demo.service;

import com.mini.spring.annotation.Component;
import com.mini.spring.web.server.Filter;
import com.mini.spring.web.server.FilterChain;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

/**
 * 示例过滤器 —— 请求日志记录
 * <p>
 * <b>学习要点：</b>
 * 过滤器在 DispatcherHandler 之前执行，可以拦截所有 HTTP 请求。
 * 这里演示了一个简单的请求/响应日志过滤器。
 * <p>
 * <b>使用方式：</b>
 * 只需实现 {@link Filter} 接口并标注 {@code @Component}，
 * 框架会在启动时自动发现并注册此过滤器。
 * <p>
 * <b>执行流程：</b>
 * <pre>
 *   HTTP 请求到达
 *       ↓
 *   [RequestLogFilter] 记录请求信息（方法、路径、耗时计时开始）
 *       ↓
 *   chain.doFilter() → 后续过滤器 → Controller 处理
 *       ↓
 *   [RequestLogFilter] 记录响应耗时
 *       ↓
 *   HTTP 响应返回
 * </pre>
 *
 * @see Filter 过滤器接口
 * @see FilterChain 过滤器链
 */
@Component
public class RequestLogFilter implements Filter {

    /**
     * 过滤处理 —— 记录请求日志和响应耗时
     * <p>
     * 在 {@code chain.doFilter()} 之前的代码是「前置处理」，
     * 在 {@code chain.doFilter()} 之后的代码是「后置处理」。
     * 这就是过滤器的强大之处：一个方法同时控制请求前后的逻辑。
     */
    @Override
    public void doFilter(HttpExchange exchange, FilterChain chain) throws IOException {
        // ===== 前置处理：记录请求信息 =====
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        long startTime = System.currentTimeMillis();

        System.out.println("[RequestLogFilter] >>> " + method + " " + path);

        // 放行请求，传递给下一个过滤器或 Controller
        chain.doFilter(exchange);

        // ===== 后置处理：记录响应耗时 =====
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("[RequestLogFilter] <<< " + method + " " + path
                + " (" + elapsed + "ms)");
    }

    /**
     * 过滤器执行顺序 —— 数值越小越先执行
     * <p>
     * 日志过滤器应最先执行（order=-1），以便记录完整的请求耗时。
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
