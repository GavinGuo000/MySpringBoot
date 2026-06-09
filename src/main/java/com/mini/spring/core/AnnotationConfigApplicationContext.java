package com.mini.spring.core;

import java.util.List;

/**
 * 注解驱动的应用上下文 —— 组合了扫描器 + Bean 工厂 + 属性解析器
 * <p>
 * <b>学习要点：</b>
 * ApplicationContext 是 Spring 的核心接口，对标 Spring 框架中的
 * {@code org.springframework.context.annotation.AnnotationConfigApplicationContext}。
 * <p>
 * 它在 BeanFactory 基础上增加了更多企业级功能（如配置加载、事件发布等）。
 * 这里是简化版的实现，采用「组合」而非「继承」的方式，将三大核心组件组装在一起：
 * <ul>
 *   <li><b>{@link PropertyResolver}</b> —— 配置属性解析器</li>
 *   <li><b>{@link BeanFactory}</b> —— IoC 容器（Bean 创建 + 依赖注入）</li>
 *   <li><b>{@link ClassPathBeanDefinitionScanner}</b> —— 类路径组件扫描器</li>
 * </ul>
 * <p>
 * <b>初始化流程：</b>
 * <pre>
 *   new AnnotationConfigApplicationContext("com.mini.demo")
 *       ↓
 *   1. 创建 PropertyResolver —— 加载 application.properties
 *       ↓
 *   2. 创建 BeanFactory —— 传入 PropertyResolver（用于 @Value 解析）
 *       ↓
 *   3. 创建 Scanner —— 组件扫描器
 *       ↓
 *   4. scanner.scan() —— 扫描包下所有 @Component/@RestController/@Configuration
 *       ↓
 *   5. beanFactory.registerBeanDefinitions() —— 注册扫描结果
 *       ↓
 *   6. beanFactory.refresh() —— 触发 Bean 创建 + 依赖注入
 *       ↓
 *   容器就绪，可以使用 getBean() 获取 Bean
 * </pre>
 * <p>
 * <b>使用方式：</b>
 * <pre>
 *   ApplicationContext ctx = new AnnotationConfigApplicationContext("com.example");
 *   MyService service = ctx.getBean(MyService.class);
 * </pre>
 *
 * @see BeanFactory IoC 容器核心
 * @see ClassPathBeanDefinitionScanner 组件扫描器
 * @see PropertyResolver 属性解析器
 */
public class AnnotationConfigApplicationContext {

    /** Bean 工厂：负责 Bean 的创建、注入和获取 */
    private final BeanFactory beanFactory;

    /** 属性解析器：加载配置文件，解析 ${key:default} 占位符 */
    private final PropertyResolver propertyResolver;

    /** 类路径扫描器：扫描指定包下的组件类 */
    private final ClassPathBeanDefinitionScanner scanner;

    /**
     * 创建应用上下文并自动初始化
     * <p>
     * 构造器即触发整个容器的初始化流程，这与 Spring 的行为一致。
     * 支持传入多个包名，会依次扫描。
     *
     * @param basePackages 要扫描的基础包名（如 "com.mini.demo"）
     */
    public AnnotationConfigApplicationContext(String... basePackages) {
        // 启动 Banner
        System.out.println("[MiniSpring] =============================================");
        System.out.println("[MiniSpring]      Mini Spring Boot 启动中...");
        System.out.println("[MiniSpring] =============================================");

        // 第 1 步：初始化属性解析器（加载 application.properties）
        // 必须先创建，因为 BeanFactory 在 @Value 注入时需要它
        this.propertyResolver = new PropertyResolver();

        // 第 2 步：初始化 Bean 工厂（传入属性解析器）
        this.beanFactory = new BeanFactory(propertyResolver);

        // 第 3 步：初始化类路径扫描器
        this.scanner = new ClassPathBeanDefinitionScanner();

        // 第 4 步：执行组件扫描，并将结果注册到 BeanFactory
        // scan() 会遍历包下所有 .class 文件，找到带 @Component/@RestController/@Configuration 的类
        // 将它们封装为 BeanDefinition 并注册到 BeanFactory 的 definitionMap 中
        List<BeanDefinition> definitions = scanner.scan(basePackages);
        beanFactory.registerBeanDefinitions(definitions);

        // 第 5 步：刷新容器 —— 这是最关键的一步
        // 会依次执行：处理 @Bean 方法 → 实例化所有 Bean → 依赖注入
        // 执行完成后，所有 Bean 已创建并注入完毕
        beanFactory.refresh();
    }

    /**
     * 根据名称获取 Bean
     * <p>
     * 对标 Spring 的 {@code context.getBean("userService")}
     *
     * @param name Bean 名称
     * @return Bean 实例
     */
    public <T> T getBean(String name) {
        return beanFactory.getBean(name);
    }

    /**
     * 根据类型获取 Bean
     * <p>
     * 对标 Spring 的 {@code context.getBean(UserService.class)}
     *
     * @param requiredType Bean 类型
     * @return Bean 实例
     */
    public <T> T getBean(Class<T> requiredType) {
        return beanFactory.getBean(requiredType);
    }

    /**
     * 根据类型获取所有匹配的 Bean
     * <p>
     * 对标 Spring 的 {@code context.getBeansOfType(UserService.class)}
     *
     * @param requiredType 目标类型
     * @return 匹配的 Bean 列表
     */
    public <T> List<T> getBeansOfType(Class<T> requiredType) {
        return beanFactory.getBeansOfType(requiredType);
    }

    /**
     * 获取内部的 BeanFactory
     * <p>
     * 用于需要直接访问 BeanFactory 的场景
     *
     * @return BeanFactory 实例
     */
    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    /**
     * 获取属性解析器
     * <p>
     * 用于读取 application.properties 中的配置值，
     * 例如在 MySpringApplication 中读取 server.port
     *
     * @return PropertyResolver 实例
     */
    public PropertyResolver getPropertyResolver() {
        return propertyResolver;
    }
}
