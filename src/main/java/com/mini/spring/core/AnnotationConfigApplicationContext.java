package com.mini.spring.core;

import java.util.List;

/**
 * 注解驱动的应用上下文 —— 组合了扫描器 + Bean 工厂 + 属性解析器
 * <p>
 * 学习要点：
 * ApplicationContext 是 Spring 的核心接口，它在 BeanFactory 基础上
 * 增加了更多企业级功能。这里是简化版的实现。
 * <p>
 * 使用方式：
 * <pre>
 *   ApplicationContext ctx = new AnnotationConfigApplicationContext("com.example");
 *   MyService service = ctx.getBean(MyService.class);
 * </pre>
 */
public class AnnotationConfigApplicationContext {

    private final BeanFactory beanFactory;
    private final PropertyResolver propertyResolver;
    private final ClassPathBeanDefinitionScanner scanner;

    /**
     * 创建应用上下文并自动初始化
     *
     * @param basePackages 要扫描的基础包名
     */
    public AnnotationConfigApplicationContext(String... basePackages) {
        System.out.println("[MiniSpring] =============================================");
        System.out.println("[MiniSpring]      Mini Spring Boot 启动中...");
        System.out.println("[MiniSpring] =============================================");

        // 1. 初始化属性解析器
        this.propertyResolver = new PropertyResolver();

        // 2. 初始化 Bean 工厂
        this.beanFactory = new BeanFactory(propertyResolver);

        // 3. 初始化扫描器
        this.scanner = new ClassPathBeanDefinitionScanner();

        // 4. 扫描组件
        List<BeanDefinition> definitions = scanner.scan(basePackages);
        beanFactory.registerBeanDefinitions(definitions);

        // 5. 刷新容器（实例化 + 依赖注入）
        beanFactory.refresh();
    }

    /**
     * 根据名称获取 Bean
     */
    public <T> T getBean(String name) {
        return beanFactory.getBean(name);
    }

    /**
     * 根据类型获取 Bean
     */
    public <T> T getBean(Class<T> requiredType) {
        return beanFactory.getBean(requiredType);
    }

    /**
     * 根据类型获取所有 Bean
     */
    public <T> List<T> getBeansOfType(Class<T> requiredType) {
        return beanFactory.getBeansOfType(requiredType);
    }

    /**
     * 获取 BeanFactory
     */
    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    /**
     * 获取属性解析器
     */
    public PropertyResolver getPropertyResolver() {
        return propertyResolver;
    }
}
