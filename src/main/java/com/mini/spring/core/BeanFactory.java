package com.mini.spring.core;

import com.mini.spring.annotation.Autowired;
import com.mini.spring.annotation.Bean;
import com.mini.spring.annotation.Configuration;
import com.mini.spring.annotation.Value;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean 工厂 —— IoC 容器的核心实现
 * <p>
 * 学习要点：
 * 1. BeanFactory 是 Spring 最底层的容器接口，负责 Bean 的创建和获取
 * 2. 依赖注入（DI）在这里完成：构造器注入 + 字段注入
 * 3. 两阶段创建：先实例化所有 Bean，再注入依赖（解决循环依赖）
 */
public class BeanFactory {

    /**
     * BeanDefinition 注册表
     */
    private final Map<String, BeanDefinition> definitionMap = new ConcurrentHashMap<>();

    /**
     * 单例 Bean 缓存（一级缓存）
     */
    private final Map<String, Object> singletonMap = new ConcurrentHashMap<>();

    /**
     * 属性解析器
     */
    private final PropertyResolver propertyResolver;

    public BeanFactory(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    // ======================== 注册 BeanDefinition ========================

    /**
     * 注册 BeanDefinition
     */
    public void registerBeanDefinition(BeanDefinition definition) {
        definitionMap.put(definition.getBeanName(), definition);
    }

    public void registerBeanDefinitions(List<BeanDefinition> definitions) {
        definitions.forEach(this::registerBeanDefinition);
    }

    // ======================== 获取 Bean ========================

    /**
     * 根据名称获取 Bean
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        Object bean = singletonMap.get(name);
        if (bean == null) {
            throw new RuntimeException("未找到 Bean: " + name);
        }
        return (T) bean;
    }

    /**
     * 根据类型获取 Bean
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        for (Object bean : singletonMap.values()) {
            if (requiredType.isInstance(bean)) {
                return (T) bean;
            }
        }
        throw new RuntimeException("未找到类型为 " + requiredType.getName() + " 的 Bean");
    }

    /**
     * 根据类型获取所有 Bean
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getBeansOfType(Class<T> requiredType) {
        List<T> result = new ArrayList<>();
        for (Object bean : singletonMap.values()) {
            if (requiredType.isInstance(bean)) {
                result.add((T) bean);
            }
        }
        return result;
    }

    // ======================== 容器初始化 ========================

    /**
     * 初始化容器 —— 创建并注入所有 Bean
     * <p>
     * 这是 Spring Bean 生命周期的核心流程（简化版）
     */
    public void refresh() {
        System.out.println("[MiniSpring] ========== 开始初始化 IoC 容器 ==========");

        // 第一阶段：扫描 @Configuration 类，注册 @Bean 方法定义的 Bean
        processBeanMethods();

        // 第二阶段：实例化所有 Bean
        instantiateBeans();

        // 第三阶段：依赖注入
        populateBeans();

        System.out.println("[MiniSpring] ========== IoC 容器初始化完成 ==========");
        System.out.println("[MiniSpring] 已注册 Bean 数量: " + singletonMap.size());
    }

    // ======================== 第一阶段：处理 @Bean 方法 ========================

    /**
     * 扫描 @Configuration 类中的 @Bean 方法，注册额外的 BeanDefinition
     */
    private void processBeanMethods() {
        List<BeanDefinition> configDefinitions = new ArrayList<>(definitionMap.values());

        for (BeanDefinition configDef : configDefinitions) {
            Class<?> configClass = configDef.getBeanClass();
            if (!configClass.isAnnotationPresent(Configuration.class)) {
                continue;
            }

            System.out.println("[MiniSpring] 处理配置类: " + configClass.getName());

            for (Method method : configClass.getDeclaredMethods()) {
                Bean beanAnn = method.getAnnotation(Bean.class);
                if (beanAnn == null) continue;

                String beanName = beanAnn.value().isEmpty()
                        ? method.getName()
                        : beanAnn.value();

                BeanDefinition bd = new BeanDefinition(method.getReturnType(), beanName);
                bd.setFactoryBeanName(configDef.getBeanName());
                bd.setFactoryMethod(method);

                registerBeanDefinition(bd);
                System.out.println("[MiniSpring] 注册 @Bean: " + beanName + " -> " + method.getReturnType().getName());
            }
        }
    }

    // ======================== 第二阶段：实例化 ========================

    /**
     * 实例化所有 Bean
     */
    private void instantiateBeans() {
        for (Map.Entry<String, BeanDefinition> entry : definitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition definition = entry.getValue();

            if (singletonMap.containsKey(beanName)) {
                continue; // 已创建
            }

            try {
                Object bean = createBeanInstance(definition);
                singletonMap.put(beanName, bean);
            } catch (Exception e) {
                throw new RuntimeException("创建 Bean 失败: " + beanName + " - " + e.getMessage(), e);
            }
        }
    }

    /**
     * 创建单个 Bean 实例
     */
    private Object createBeanInstance(BeanDefinition definition) throws Exception {
        // 方式1：通过 @Bean 工厂方法创建
        if (definition.hasFactoryMethod()) {
            return createFromFactoryMethod(definition);
        }
        // 方式2：通过构造器创建
        return createFromConstructor(definition);
    }

    /**
     * 通过 @Bean 工厂方法创建
     */
    private Object createFromFactoryMethod(BeanDefinition definition) throws Exception {
        Object configBean = singletonMap.get(definition.getFactoryBeanName());
        if (configBean == null) {
            // 配置类还没实例化，先创建它
            BeanDefinition configDef = definitionMap.get(definition.getFactoryBeanName());
            configBean = createBeanInstance(configDef);
            singletonMap.put(definition.getFactoryBeanName(), configBean);
        }

        Method method = definition.getFactoryMethod();
        method.setAccessible(true);

        // 解析方法参数的依赖
        Object[] args = resolveMethodArguments(method);
        return method.invoke(configBean, args);
    }

    /**
     * 通过构造器创建
     */
    private Object createFromConstructor(BeanDefinition definition) throws Exception {
        Class<?> clazz = definition.getBeanClass();
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();

        // 优先使用带 @Autowired 的构造器
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Autowired.class)) {
                Object[] args = resolveConstructorArguments(constructor);
                constructor.setAccessible(true);
                return constructor.newInstance(args);
            }
        }

        // 使用默认无参构造器
        Constructor<?> defaultConstructor = clazz.getDeclaredConstructor();
        defaultConstructor.setAccessible(true);
        return defaultConstructor.newInstance();
    }

    /**
     * 解析构造器参数（依赖查找）
     */
    private Object[] resolveConstructorArguments(Constructor<?> constructor) {
        Parameter[] parameters = constructor.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            args[i] = findBeanByType(parameters[i].getType());
        }
        return args;
    }

    /**
     * 解析方法参数（依赖查找）
     */
    private Object[] resolveMethodArguments(Method method) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            args[i] = findBeanByType(parameters[i].getType());
        }
        return args;
    }

    // ======================== 第三阶段：依赖注入 ========================

    /**
     * 对所有 Bean 进行依赖注入
     */
    private void populateBeans() {
        for (Map.Entry<String, Object> entry : singletonMap.entrySet()) {
            String beanName = entry.getKey();
            Object bean = entry.getValue();

            try {
                populateBean(beanName, bean);
            } catch (Exception e) {
                throw new RuntimeException("依赖注入失败: " + beanName + " - " + e.getMessage(), e);
            }
        }
    }

    /**
     * 对单个 Bean 注入字段依赖
     */
    private void populateBean(String beanName, Object bean) throws Exception {
        Class<?> clazz = bean.getClass();

        // 遍历所有字段（包括父类）
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                // 1. 处理 @Autowired 字段注入
                if (field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true);
                    Object dependency = findBeanByType(field.getType());
                    field.set(bean, dependency);
                    System.out.println("[MiniSpring] 注入依赖: " + beanName + "." + field.getName()
                            + " <- " + dependency.getClass().getSimpleName());
                }

                // 2. 处理 @Value 属性注入
                if (field.isAnnotationPresent(Value.class)) {
                    field.setAccessible(true);
                    Value valueAnn = field.getAnnotation(Value.class);
                    Object value = propertyResolver.resolve(valueAnn.value(), field.getType());
                    field.set(bean, value);
                    System.out.println("[MiniSpring] 注入属性: " + beanName + "." + field.getName()
                            + " = " + value);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    // ======================== 依赖查找 ========================

    /**
     * 按类型查找 Bean
     */
    private Object findBeanByType(Class<?> requiredType) {
        // 先按名称查找
        String typeName = Character.toLowerCase(requiredType.getSimpleName().charAt(0))
                + requiredType.getSimpleName().substring(1);
        Object bean = singletonMap.get(typeName);
        if (bean != null && requiredType.isInstance(bean)) {
            return bean;
        }

        // 按类型查找
        for (Object candidate : singletonMap.values()) {
            if (requiredType.isInstance(candidate)) {
                return candidate;
            }
        }

        throw new RuntimeException("未找到类型为 " + requiredType.getName() + " 的 Bean，无法完成注入");
    }

    // ======================== 工具方法 ========================

    /**
     * 获取所有已注册 Bean 的名称
     */
    public List<String> getBeanNames() {
        return new ArrayList<>(definitionMap.keySet());
    }

    /**
     * 检查是否包含指定名称的 Bean
     */
    public boolean containsBean(String name) {
        return singletonMap.containsKey(name);
    }
}
