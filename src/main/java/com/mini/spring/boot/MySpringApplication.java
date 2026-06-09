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
 * 这是整个框架的入口类，对标 Spring Boot 的 {@code SpringApplication}。
 * 它负责把 IoC 容器、Web 路由、内嵌服务器三大模块串联起来，一键完成启动。
 * <p>
 * <b>学习要点：</b>
 * Spring Boot 的 {@code SpringApplication.run()} 本质上做了以下几件事：
 * <ol>
 *   <li>确定扫描的基础包（basePackage），知道去哪里找组件</li>
 *   <li>创建 ApplicationContext（IoC 容器），完成组件扫描、注册、实例化和依赖注入</li>
 *   <li>收集所有 {@code @RestController}，注册到路由映射表（URL -> 处理方法）</li>
 *   <li>读取配置（如 server.port），启动内嵌 Web 服务器监听 HTTP 请求</li>
 * </ol>
 * <p>
 * <b>启动流程图：</b>
 * <pre>
 *   main() 调用
 *       ↓
 *   确定扫描包（主类所在包）
 *       ↓
 *   创建 AnnotationConfigApplicationContext
 *       ├── ClassPathBeanDefinitionScanner 扫描 @Component/@RestController
 *       ├── BeanFactory 注册 BeanDefinition
 *       ├── 实例化所有 Bean 并完成 @Autowired 依赖注入
 *       └── 处理 @Configuration + @Bean 配置类
 *       ↓
 *   遍历所有 Bean，找到 @RestController → 注册到 HandlerMapping
 *       ↓
 *   读取 server.port 配置 → 启动 EmbeddedWebServer
 *       ↓
 *   应用就绪，等待 HTTP 请求
 * </pre>
 * <p>
 * <b>使用方式：</b>
 * <pre>
 *   public static void main(String[] args) {
 *       MySpringApplication.run(App.class, args);
 *   }
 * </pre>
 *
 * @see AnnotationConfigApplicationContext IoC 容器核心
 * @see HandlerMapping 路由映射
 * @see EmbeddedWebServer 内嵌 Web 服务器
 */
public class MySpringApplication {

    /** 默认 HTTP 端口号，当 application.properties 未配置 server.port 时使用 */
    private static final int DEFAULT_PORT = 8080;

    /**
     * 启动 Mini Spring Boot 应用（核心入口方法）
     * <p>
     * 该方法按顺序完成：容器初始化 → 路由注册 → 配置读取 → 服务器启动
     *
     * @param primarySource 主启动类（用于确定组件扫描的基础包路径，
     *                      例如传入 {@code MySpringBootApplication.class}，
     *                      则会扫描 {@code com.mini.demo} 包下所有组件）
     * @param args          命令行参数（保留扩展，暂未使用）
     * @return 初始化完成的 IoC 容器（ApplicationContext），可通过它获取任意 Bean
     */
    public static AnnotationConfigApplicationContext run(Class<?> primarySource, String[] args) {
        // 记录启动开始时间，用于最后打印启动耗时
        long startTime = System.currentTimeMillis();

        // ======================== 第 1 步：确定扫描包 ========================
        // 取主类所在的包名作为组件扫描的根路径
        // 例如：com.mini.demo.MySpringBootApplication → 扫描 com.mini.demo 及其子包
        // 这与 Spring Boot 的默认行为一致：@SpringBootApplication 标注的类所在包即为扫描起点
        String basePackage = primarySource.getPackageName(); // 获取这个类所在的包！！！！！
        System.out.println("[MiniSpring] 扫描包: " + basePackage);

        // ======================== 第 2 步：创建并初始化 IoC 容器 ========================
        // AnnotationConfigApplicationContext 是整个框架的核心，构造时会自动完成：
        //   (a) PropertyResolver —— 加载 application.properties 配置文件
        //   (b) ClassPathBeanDefinitionScanner —— 扫描 basePackage 下带 @Component/@RestController 的类
        //   (c) BeanFactory —— 将所有 BeanDefinition 实例化为 Bean 对象
        //   (d) 依赖注入 —— 自动为 @Autowired 字段注入对应的 Bean 实例
        //   (e) @Configuration 处理 —— 执行配置类中 @Bean 方法，注册额外的 Bean
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(basePackage);

        // ======================== 第 3 步：注册路由映射 ========================
        // HandlerMapping 负责维护「URL 路径 → Controller 方法」的映射关系
        // 遍历容器中所有 Bean，筛选出带 @RestController 注解的类
        // 解析其中的 @GetMapping/@PostMapping，注册到 HandlerMapping 中
        HandlerMapping handlerMapping = new HandlerMapping();
        // getBeansOfType(Object.class) 获取容器中所有 Bean（Object 是所有类的父类）
        List<?> controllers = context.getBeansOfType(Object.class);
        for (Object bean : controllers) {
            // 判断 Bean 的类上是否标注了 @RestController
            if (bean.getClass().isAnnotationPresent(RestController.class)) {
                // 将该 Controller 中所有带 @GetMapping/@PostMapping 的方法注册到路由表
                handlerMapping.registerController(bean);
            }
        }

        // ======================== 第 4 步：读取端口配置 ========================
        // 从 PropertyResolver 中获取 application.properties 里 server.port 的值
        // 如果未配置，则使用默认端口 8080
        int port = DEFAULT_PORT;
        PropertyResolver resolver = context.getPropertyResolver();
        String portStr = resolver.getProperty("server.port", String.valueOf(DEFAULT_PORT));
        port = Integer.parseInt(portStr);

        // ======================== 第 5 步：启动内嵌 Web 服务器 ========================
        // EmbeddedWebServer 基于 JDK 自带的 com.sun.net.httpserver.HttpServer
        // 它在指定端口监听 HTTP 请求，并将请求分发给 HandlerMapping 处理
        try {
            EmbeddedWebServer webServer = new EmbeddedWebServer(port, handlerMapping);
            webServer.start();

            // 注册 JVM 关闭钩子（Shutdown Hook）
            // 当用户按 Ctrl+C 或 JVM 关闭时，会自动执行此线程来优雅关闭服务器
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[MiniSpring] 正在关闭应用...");
                webServer.stop();
            }));

        } catch (Exception e) {
            System.err.println("[MiniSpring] 启动失败: " + e.getMessage());
            e.printStackTrace();
        }

        // 计算并打印启动耗时
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("[MiniSpring] 启动耗时: " + elapsed + "ms");

        return context;
    }
}
