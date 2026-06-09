package com.mini.spring.web.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * 内嵌 Web 服务器 —— 基于 JDK 内置的 HttpServer
 * <p>
 * 学习要点：
 * Spring Boot 内嵌 Tomcat/Jetty 的原理与此类似：
 * 在应用启动时创建一个 HTTP 服务器，将请求分发给 Spring MVC 处理。
 */
public class EmbeddedWebServer {

    private final int port;
    private HttpServer server;
    private final HandlerMapping handlerMapping;

    public EmbeddedWebServer(int port, HandlerMapping handlerMapping) {
        this.port = port;
        this.handlerMapping = handlerMapping;
    }

    /**
     * 启动服务器
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // 注册请求分发处理器，处理所有路径
        DispatcherHandler dispatcher = new DispatcherHandler(handlerMapping);
        server.createContext("/", dispatcher);

        // 使用线程池处理并发请求
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println();
        System.out.println("[MiniSpring] =============================================");
        System.out.println("[MiniSpring]   🚀 Mini Spring Boot 启动成功!");
        System.out.println("[MiniSpring]   📍 地址: http://localhost:" + port);
        System.out.println("[MiniSpring] =============================================");
        System.out.println();
    }

    /**
     * 停止服务器
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[MiniSpring] 服务器已停止");
        }
    }

    public int getPort() {
        return port;
    }
}
