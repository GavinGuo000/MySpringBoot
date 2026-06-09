package com.mini.spring.boot;

import com.mini.spring.core.AnnotationConfigApplicationContext;
import com.mini.spring.core.PropertyResolver;
import com.mini.spring.web.annotation.RestController;
import com.mini.spring.web.server.EmbeddedWebServer;
import com.mini.spring.web.server.HandlerMapping;

import java.util.List;

/**
 * Mini Spring Boot 启动器 —— 一键启动整个应用
 * <p>
 * 学习要点：
 * SpringApplication.run() 是 Spring Boot 的入口，它做了以下事情：
 * 1. 创建 ApplicationContext（IoC 容器）
 * 2. 扫描并注册所有组件
 * 3. 启动内嵌 Web 服务器
 * <p>
 * 使用方式：
 * <pre>
 *   public static void main(String[] args) {
 *       MySpringApplication.run(App.class, args);
 *   }
 * </pre>
 */
public class MySpringApplication {

    private static final int DEFAULT_PORT = 8080;

    /**
     * 启动 Mini Spring Boot 应用
     *
     * @param primarySource 主类（用于确定扫描包）
     * @param args          命令行参数
     * @return 应用上下文
     */
    public static AnnotationConfigApplicationContext run(Class<?> primarySource, String[] args) {
        long startTime = System.currentTimeMillis();

        // 1. 确定扫描包（主类所在的包）
        String basePackage = primarySource.getPackageName();
        System.out.println("[MiniSpring] 扫描包: " + basePackage);

        // 2. 创建并初始化 IoC 容器
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(basePackage);

        // 3. 注册所有 @RestController 到路由映射
        HandlerMapping handlerMapping = new HandlerMapping();
        List<?> controllers = context.getBeansOfType(Object.class);
        for (Object bean : controllers) {
            if (bean.getClass().isAnnotationPresent(RestController.class)) {
                handlerMapping.registerController(bean);
            }
        }

        // 4. 获取端口配置
        int port = DEFAULT_PORT;
        PropertyResolver resolver = context.getPropertyResolver();
        String portStr = resolver.getProperty("server.port", String.valueOf(DEFAULT_PORT));
        port = Integer.parseInt(portStr);

        // 5. 启动内嵌 Web 服务器
        try {
            EmbeddedWebServer webServer = new EmbeddedWebServer(port, handlerMapping);
            webServer.start();

            // 注册关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[MiniSpring] 正在关闭应用...");
                webServer.stop();
            }));

        } catch (Exception e) {
            System.err.println("[MiniSpring] 启动失败: " + e.getMessage());
            e.printStackTrace();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("[MiniSpring] 启动耗时: " + elapsed + "ms");

        return context;
    }
}
