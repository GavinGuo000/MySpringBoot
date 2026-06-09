package com.mini.spring.web.server;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;

/**
 * 过滤器链 —— 对标 Servlet 规范中的 javax.servlet.FilterChain
 * <p>
 * <b>学习要点：</b>
 * FilterChain 是「责任链模式」的经典实现。
 * 它持有所有 Filter 的有序列表，通过 {@code doFilter()} 依次调用每个 Filter，
 * 最终将请求传递给 DispatcherHandler。
 * <p>
 * <b>工作原理：</b>
 * <pre>
 *   FilterChain 内部维护一个索引 index（当前执行到第几个 Filter）
 *
 *   doFilter(exchange):
 *       if (index < filters.size())
 *           → 调用 filters[index].doFilter(exchange, this)  // 传递当前链
 *           → index++（下次调用 doFilter 时执行下一个 Filter）
 *       else
 *           → 调用 finalHandler.handle(exchange)  // 所有 Filter 执行完毕，进入分发器
 * </pre>
 * <p>
 * <b>关键设计：</b>
 * <ul>
 *   <li>每个 Filter 的 {@code chain.doFilter()} 调用会触发下一个 Filter 的执行</li>
 *   <li>如果某个 Filter 不调用 {@code chain.doFilter()}，请求链就此中断</li>
 *   <li>Filter 可以在 {@code chain.doFilter()} 前后添加自定义逻辑（前置/后置处理）</li>
 * </ul>
 *
 * @see Filter 过滤器接口
 * @see DispatcherHandler 最终请求处理器
 */
public class FilterChain {

    /** 所有过滤器（按 order 排序后的有序列表） */
    private final List<Filter> filters;

    /** 最终的请求处理器（所有 Filter 执行完后调用） */
    private final HttpHandler finalHandler;

    /**
     * 当前执行到第几个 Filter 的索引
     * <p>
     * 每次调用 doFilter() 时，先取出当前索引的 Filter 执行，再将索引 +1。
     * 这样下一次 Filter 调用 chain.doFilter() 时，就会执行下一个 Filter。
     */
    private int index = 0;

    /**
     * 创建过滤器链
     *
     * @param filters      按 order 排序后的过滤器列表
     * @param finalHandler 所有过滤器执行完毕后的最终处理器（通常是 DispatcherHandler）
     */
    public FilterChain(List<Filter> filters, HttpHandler finalHandler) {
        this.filters = filters;
        this.finalHandler = finalHandler;
    }

    /**
     * 执行过滤器链中的下一个环节
     * <p>
     * 对标 Servlet 的 {@code FilterChain.doFilter()}。
     * <ul>
     *   <li>如果还有未执行的 Filter → 调用下一个 Filter</li>
     *   <li>如果所有 Filter 已执行完 → 调用最终处理器（DispatcherHandler）</li>
     * </ul>
     *
     * @param exchange HTTP 请求/响应对象
     */
    public void doFilter(HttpExchange exchange) throws IOException {
        if (index < filters.size()) {
            // 取出当前 Filter，索引 +1
            Filter filter = filters.get(index++);
            // 调用 Filter，将当前链对象传递进去（Filter 通过 chain.doFilter 触发下一个）
            filter.doFilter(exchange, this);
        } else {
            // 所有 Filter 已执行完毕，调用最终的请求处理器
            finalHandler.handle(exchange);
        }
    }

    /**
     * 函数式接口 —— 最终的 HTTP 处理器
     * <p>
     * 当所有 Filter 执行完毕后，FilterChain 会调用此接口处理请求。
     * 通常由 DispatcherHandler 实现此接口。
     */
    @FunctionalInterface
    public interface HttpHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
