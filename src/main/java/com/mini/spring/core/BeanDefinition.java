package com.mini.spring.core;

import java.lang.reflect.Method;

/**
 * Bean 定义 —— 描述一个 Bean 的元信息（Metadata）
 * <p>
 * <b>学习要点：</b>
 * BeanDefinition 是 Spring 的核心抽象之一，对标 Spring 框架中的
 * {@code org.springframework.beans.factory.config.BeanDefinition}。
 * <p>
 * 它把「如何创建一个 Bean」的信息封装起来，供 BeanFactory 使用。
 * 可以把它理解为一个「Bean 的蓝图」，包含了：
 * <ul>
 *   <li><b>beanClass</b> —— Bean 的类型（对应哪个 Java 类）</li>
 *   <li><b>beanName</b> —— Bean 的名称（在容器中的唯一标识符）</li>
 *   <li><b>factoryBeanName + factoryMethod</b> —— 如果是通过 @Bean 方法创建的，
 *       记录是哪个配置类的哪个方法负责创建</li>
 * </ul>
 * <p>
 * <b>两种创建方式：</b>
 * <pre>
 *   方式 1：组件扫描（@Component / @RestController）
 *          → 只有 beanClass + beanName，通过构造器实例化
 *
 *   方式 2：配置类（@Configuration + @Bean）
 *          → 额外设置 factoryBeanName + factoryMethod，通过工厂方法实例化
 * </pre>
 *
 * @see BeanFactory 负责根据 BeanDefinition 创建 Bean 实例
 * @see ClassPathBeanDefinitionScanner 负责扫描并生成 BeanDefinition
 */
public class BeanDefinition {

    /**
     * Bean 的类型（Java 类）
     * <p>
     * 对于 @Component 扫描的类，这就是被标注的类本身；
     * 对于 @Bean 方法，这是方法的返回类型。
     */
    private final Class<?> beanClass;

    /**
     * Bean 的名称（在容器中的唯一标识）
     * <p>
     * 命名规则：
     * <ul>
     *   <li>默认：类名首字母小写，如 UserService → "userService"</li>
     *   <li>自定义：通过 @Component("myService") 指定</li>
     *   <li>@Bean：方法名作为 beanName，或通过 @Bean("myBean") 指定</li>
     * </ul>
     */
    private final String beanName;

    /**
     * 工厂 Bean 的名称（仅用于 @Bean 方式创建）
     * <p>
     * 记录 @Bean 方法所在的 @Configuration 类的 beanName。
     * 例如：AppConfig 类中有一个 @Bean 方法，则 factoryBeanName = "appConfig"
     */
    private String factoryBeanName;

    /**
     * 工厂方法（仅用于 @Bean 方式创建）
     * <p>
     * 记录标注了 @Bean 的 Method 对象。
     * BeanFactory 会调用 configBean.factoryMethod() 来创建 Bean 实例。
     */
    private Method factoryMethod;

    /**
     * 创建 BeanDefinition
     *
     * @param beanClass Bean 的类型
     * @param beanName  Bean 的名称
     */
    public BeanDefinition(Class<?> beanClass, String beanName) {
        this.beanClass = beanClass;
        this.beanName = beanName;
    }

    /** 获取 Bean 的类型 */
    public Class<?> getBeanClass() {
        return beanClass;
    }

    /** 获取 Bean 的名称 */
    public String getBeanName() {
        return beanName;
    }

    /** 获取工厂 Bean 的名称（@Bean 方式专用） */
    public String getFactoryBeanName() {
        return factoryBeanName;
    }

    /** 设置工厂 Bean 的名称（@Bean 方式专用） */
    public void setFactoryBeanName(String factoryBeanName) {
        this.factoryBeanName = factoryBeanName;
    }

    /** 获取工厂方法（@Bean 方式专用） */
    public Method getFactoryMethod() {
        return factoryMethod;
    }

    /** 设置工厂方法（@Bean 方式专用） */
    public void setFactoryMethod(Method factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    /**
     * 判断是否通过 @Bean 工厂方法创建
     * <p>
     * 如果返回 true，BeanFactory 会调用 factoryBeanName 对应配置类的 factoryMethod 来创建实例；
     * 如果返回 false，BeanFactory 会通过 beanClass 的构造器来创建实例。
     */
    public boolean hasFactoryMethod() {
        return factoryMethod != null;
    }

    @Override
    public String toString() {
        return "BeanDefinition{" +
                "beanClass=" + beanClass.getName() +
                ", beanName='" + beanName + '\'' +
                (hasFactoryMethod() ? ", factoryMethod=" + factoryMethod.getName() : "") +
                '}';
    }
}
