package com.mini.spring.web.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * 内嵌 Web 服务器 —— 基于 JDK 内置的 HttpServer
 * <p>
 * <b>学习要点：</b>
 * Spring Boot 内嵌 Tomcat/Jetty/Undertow 的原理与此类似：
 * 在应用启动时创建一个 HTTP 服务器，将所有请求分发给 Spring MVC 的 DispatcherServlet 处理。
 * <p>
 * 这里使用 JDK 自带的 {@code com.sun.net.httpserver.HttpServer}（无需外部依赖），
 * 它提供了基础的 HTTP 服务器功能，支持 GET/POST 等 HTTP 方法。
 * <p>
 * <b>与 Spring Boot 的对比：</b>
 * <table>
 *   <tr><th>功能</th><th>Spring Boot</th><th>Mini Spring Boot</th></tr>
 *   <tr><td>服务器</td><td>Tomcat / Jetty / Undertow</td><td>JDK HttpServer</td></tr>
 *   <tr><td>请求分发</td><td>DispatcherServlet</td><td>DispatcherHandler</td></tr>
 *   <tr><td>路由映射</td><td>HandlerMapping</td><td>HandlerMapping（简化版）</td></tr>
 *   <tr><td>JSON 序列化</td><td>Jackson</td><td>Gson</td></tr>
 * </table>
 * <p>
 * <b>启动流程：</b>
 * <pre>
 *   new EmbeddedWebServer(8080, handlerMapping)
 *       ↓
 *   start()
 *       ↓
 *   1. HttpServer.create(端口) —— 创建 HTTP 服务器实例
 *   2. 注册 DispatcherHandler 到根路径 "/"
 *   3. 设置线程池（10 个线程处理并发）
 *   4. server.start() —— 开始监听端口
 * </pre>
 *
 * @see DispatcherHandler 请求分发处理器
 * @see HandlerMapping 路由映射表
 */
public class EmbeddedWebServer {

    /** 服务器监听端口 */
    private final int port;

    /** JDK 内置的 HTTP 服务器实例 */
    private HttpServer server;

    /** 路由映射表，传递给 DispatcherHandler 用于查找路由 */
    private final HandlerMapping handlerMapping;

    /** 过滤器注册列表 */
    private List<FilterRegistrationBean> filterRegistrations = new ArrayList<>();

    /** 拦截器注册表 */
    private InterceptorRegistry interceptorRegistry = new InterceptorRegistry();

    /**
     * 创建内嵌 Web 服务器
     *
     * @param port           监听端口（如 8080）
     * @param handlerMapping 路由映射表
     */
    public EmbeddedWebServer(int port, HandlerMapping handlerMapping) {
        this.port = port;
        this.handlerMapping = handlerMapping;
    }

    /**
     * 创建内嵌 Web 服务器（带过滤器和拦截器）
     *
     * @param port                监听端口
     * @param handlerMapping      路由映射表
     * @param filterRegistrations 过滤器注册列表
     * @param interceptorRegistry 拦截器注册表
     */
    public EmbeddedWebServer(int port, HandlerMapping handlerMapping,
                             List<FilterRegistrationBean> filterRegistrations,
                             InterceptorRegistry interceptorRegistry) {
        this.port = port;
        this.handlerMapping = handlerMapping;
        this.filterRegistrations = filterRegistrations;
        this.interceptorRegistry = interceptorRegistry;
    }

    /**
     * 启动服务器
     * <p>
     * 对标 Spring Boot 的 {@code TomcatWebServer.start()}。
     * 步骤：
     * <ol>
     *   <li>创建 HttpServer 实例并绑定端口</li>
     *   <li>注册 DispatcherHandler 到根路径 "/"（所有请求都会经过它）</li>
     *   <li>设置固定大小线程池（10 个线程）处理并发请求</li>
     *   <li>启动服务器，开始接收请求</li>
     * </ol>
     *
     * @throws IOException 端口被占用或网络异常时抛出
     */
    public void start() throws IOException {
        // 创建 HttpServer 实例，绑定到指定端口
        // 第二个参数 0 表示使用默认的 backlog（待处理连接队列长度）
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // 创建 DispatcherHandler 并注册到根路径 "/"
        // 这意味着所有 HTTP 请求都会被 DispatcherHandler 处理
        DispatcherHandler dispatcher = new DispatcherHandler(handlerMapping, filterRegistrations, interceptorRegistry);
        server.createContext("/", dispatcher);

        // 使用固定大小线程池处理并发请求
        // 对标 Spring Boot 内嵌 Tomcat 的线程池配置（默认 200 个线程）
        server.setExecutor(Executors.newFixedThreadPool(10));

        // 启动服务器，开始监听
        server.start();

        // 打印启动成功信息
        System.out.println();
        System.out.println("[MiniSpring] =============================================");
        System.out.println("[MiniSpring]   🚀 Mini Spring Boot 启动成功!");
        System.out.println("[MiniSpring]   📍 地址: http://localhost:" + port);
        System.out.println("[MiniSpring] =============================================");
        System.out.println();
    }

    /**
     * 停止服务器
     * <p>
     * 对标 Spring Boot 的 {@code TomcatWebServer.stop()}。
     * 参数 0 表示立即停止（不等待现有请求处理完成）。
     * <p>
     * 在 MySpringApplication 中通过 JVM Shutdown Hook 调用此方法，
     * 确保应用关闭时服务器能优雅停止。
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[MiniSpring] 服务器已停止");
        }
    }

    /**
     * 获取服务器监听端口
     *
     * @return 端口号
     */
    public int getPort() {
        return port;
    }
}
