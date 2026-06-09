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
 * <b>学习要点：</b>
 * 这是整个迷你框架中最核心的类，对标 Spring 的
 * {@code DefaultListableBeanFactory} + {@code AbstractAutowireCapableBeanFactory}。
 * <p>
 * <b>核心职责：</b>
 * <ol>
 *   <li><b>BeanDefinition 注册</b> —— 维护所有 Bean 的元数据</li>
 *   <li><b>Bean 实例化</b> —— 通过构造器或 @Bean 工厂方法创建 Bean 实例</li>
 *   <li><b>依赖注入（DI）</b> —— 处理 @Autowired 字段注入 + @Value 属性注入</li>
 *   <li><b>Bean 获取</b> —— 按名称或类型从容器中获取 Bean</li>
 * </ol>
 * <p>
 * <b>Bean 生命周期（简化版 Spring）：</b>
 * <pre>
 *   refresh() 调用
 *       ↓
 *   第 1 阶段：processBeanMethods()
 *       扫描 @Configuration 类，注册 @Bean 方法定义的 BeanDefinition
 *       ↓
 *   第 2 阶段：instantiateBeans()
 *       遍历所有 BeanDefinition，通过构造器或工厂方法创建 Bean 实例
 *       此时 Bean 已创建，但 @Autowired 字段还未注入（都是 null）
 *       ↓
 *   第 3 阶段：populateBeans()
 *       遍历所有 Bean，为 @Autowired 字段注入依赖，为 @Value 字段注入配置值
 *       ↓
 *   容器就绪
 * </pre>
 * <p>
 * <b>两阶段创建的设计意义：</b>
 * 先全部实例化，再统一注入，这样即使存在循环依赖（A→B→A），
 * 在注入阶段也能正确获取已创建的实例（类似 Spring 的三级缓存简化版）。
 *
 * @see BeanDefinition Bean 的元数据描述
 * @see AnnotationConfigApplicationContext 组合使用 BeanFactory 的上层容器
 */
public class BeanFactory {

    /**
     * BeanDefinition 注册表
     * <p>
     * 存储所有 Bean 的元数据（包括扫描来的和 @Bean 方法定义的）。
     * Key = beanName（如 "userService"），Value = BeanDefinition
     */
    private final Map<String, BeanDefinition> definitionMap = new ConcurrentHashMap<>();

    /**
     * 单例 Bean 缓存（一级缓存）
     * <p>
     * 对标 Spring 的 {@code singletonObjects}（一级缓存）。
     * 存储所有已创建并初始化完成的 Bean 实例。
     * Key = beanName，Value = Bean 实例
     */
    private final Map<String, Object> singletonMap = new ConcurrentHashMap<>();

    /**
     * 属性解析器
     * <p>
     * 用于解析 @Value("${key:default}") 注解中的占位符表达式
     */
    private final PropertyResolver propertyResolver;

    public BeanFactory(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    // ======================== 注册 BeanDefinition ========================

    /**
     * 注册单个 BeanDefinition
     * <p>
     * 将 BeanDefinition 放入 definitionMap，后续 refresh() 时会根据它创建 Bean。
     *
     * @param definition Bean 的元数据描述
     */
    public void registerBeanDefinition(BeanDefinition definition) {
        definitionMap.put(definition.getBeanName(), definition);
    }

    /**
     * 批量注册 BeanDefinition（扫描结果通常使用此方法）
     *
     * @param definitions BeanDefinition 列表
     */
    public void registerBeanDefinitions(List<BeanDefinition> definitions) {
        definitions.forEach(this::registerBeanDefinition);
    }

    // ======================== 获取 Bean ========================

    /**
     * 根据名称获取 Bean
     * <p>
     * 对标 Spring 的 {@code ApplicationContext.getBean(String name)}
     *
     * @param name Bean 名称
     * @return Bean 实例
     * @throws RuntimeException 当找不到指定名称的 Bean 时
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
     * <p>
     * 对标 Spring 的 {@code ApplicationContext.getBean(Class<T> requiredType)}
     * 遍历所有 Bean，找到第一个类型匹配的实例。
     *
     * @param requiredType 目标类型
     * @return Bean 实例
     * @throws RuntimeException 当找不到匹配类型的 Bean 时
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
     * 根据类型获取所有匹配的 Bean
     * <p>
     * 对标 Spring 的 {@code ApplicationContext.getBeansOfType(Class<T> type)}
     * 例如：getBeansOfType(Object.class) 返回所有 Bean
     *
     * @param requiredType 目标类型
     * @return 匹配的 Bean 列表
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
     * 这是 Spring Bean 生命周期的核心流程（简化版），对标 Spring 的
     * {@code AbstractApplicationContext.refresh()} 方法。
     * <p>
     * 三个阶段顺序不能变：
     * <ol>
     *   <li><b>processBeanMethods()</b> —— 必须先处理 @Bean 方法，注册它们定义的 BeanDefinition</li>
     *   <li><b>instantiateBeans()</b> —— 然后才能统一实例化所有 Bean（包括 @Bean 定义的）</li>
     *   <li><b>populateBeans()</b> —— 最后统一注入依赖（此时所有 Bean 已创建）</li>
     * </ol>
     */
    public void refresh() {
        System.out.println("[MiniSpring] ========== 开始初始化 IoC 容器 ==========");

        // 第一阶段：扫描 @Configuration 类，注册 @Bean 方法定义的 BeanDefinition
        // 例如：AppConfig 中的 @Bean 方法 "greetingService()" 会生成一个新的 BeanDefinition
        processBeanMethods();

        // 第二阶段：遍历所有 BeanDefinition，通过构造器或工厂方法创建 Bean 实例
        // 注意：此时 Bean 已创建，但字段上的 @Autowired 还没有注入
        instantiateBeans();

        // 第三阶段：遍历所有 Bean，注入 @Autowired 依赖和 @Value 配置值
        populateBeans();

        System.out.println("[MiniSpring] ========== IoC 容器初始化完成 ==========");
        System.out.println("[MiniSpring] 已注册 Bean 数量: " + singletonMap.size());
    }

    // ======================== 第一阶段：处理 @Bean 方法 ========================

    /**
     * 扫描 @Configuration 类中的 @Bean 方法，注册额外的 BeanDefinition
     * <p>
     * 对标 Spring 的 {@code ConfigurationClassPostProcessor}，
     * 它负责解析 @Configuration 类中的 @Bean 方法。
     * <p>
     * 处理流程：
     * <pre>
     *   @Configuration
     *   public class AppConfig {
     *       @Bean("greetingService")    ← 扫描到这个方法
     *       public GreetingService greetingService() {
     *           return new GreetingService();
     *       }
     *   }
     *       ↓
     *   创建 BeanDefinition {
     *       beanClass = GreetingService.class,
     *       beanName = "greetingService",
     *       factoryBeanName = "appConfig",     ← 配置类的 beanName
     *       factoryMethod = greetingService()  ← @Bean 方法
     *   }
     * </pre>
     */
    private void processBeanMethods() {
        // 复制当前 definitionMap 的值，避免遍历时并发修改
        List<BeanDefinition> configDefinitions = new ArrayList<>(definitionMap.values());

        for (BeanDefinition configDef : configDefinitions) {
            Class<?> configClass = configDef.getBeanClass();
            // 只处理带 @Configuration 注解的类
            if (!configClass.isAnnotationPresent(Configuration.class)) {
                continue;
            }

            System.out.println("[MiniSpring] 处理配置类: " + configClass.getName());

            // 遍历配置类的所有方法，找到带 @Bean 注解的
            for (Method method : configClass.getDeclaredMethods()) {
                Bean beanAnn = method.getAnnotation(Bean.class);
                if (beanAnn == null) continue;

                // 确定 Bean 名称：优先用 @Bean("name") 指定，否则用方法名
                String beanName = beanAnn.value().isEmpty()
                        ? method.getName()
                        : beanAnn.value();

                // 创建 BeanDefinition，设置工厂方法信息
                BeanDefinition bd = new BeanDefinition(method.getReturnType(), beanName);
                bd.setFactoryBeanName(configDef.getBeanName()); // 哪个配置类
                bd.setFactoryMethod(method);                     // 哪个方法

                // 注册到 definitionMap
                registerBeanDefinition(bd);
                System.out.println("[MiniSpring] 注册 @Bean: " + beanName + " -> " + method.getReturnType().getName());
            }
        }
    }

    // ======================== 第二阶段：实例化 ========================
    
    /**
     * 实例化所有 Bean
     * <p>
     * 遍历 definitionMap 中所有 BeanDefinition，为每个创建对应的 Bean 实例。
     * 如果 Bean 已存在（可能被工厂方法提前创建），则跳过。
     */
    private void instantiateBeans() {
        for (Map.Entry<String, BeanDefinition> entry : definitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition definition = entry.getValue();
    
            if (singletonMap.containsKey(beanName)) {
                continue; // 已创建（可能被其他 Bean 的工厂方法提前触发创建）
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
     * <p>
     * 根据 BeanDefinition 的信息，选择合适的创建方式：
     * <ul>
     *   <li>有 factoryMethod → 通过 @Bean 工厂方法创建</li>
     *   <li>无 factoryMethod → 通过构造器创建（默认无参或 @Autowired 构造器）</li>
     * </ul>
     *
     * @param definition Bean 的元数据
     * @return 新创建的 Bean 实例
     */
    private Object createBeanInstance(BeanDefinition definition) throws Exception {
        // 方式 1：通过 @Bean 工厂方法创建
        if (definition.hasFactoryMethod()) {
            return createFromFactoryMethod(definition);
        }
        // 方式 2：通过构造器创建（@Component 扫描的类）
        return createFromConstructor(definition);
    }
    
    /**
     * 通过 @Bean 工厂方法创建 Bean
     * <p>
     * 对标 Spring 的 {@code ConfigurationClassEnhancer} 简化版。
     * 流程：
     * <pre>
     *   1. 获取配置类实例（如果还没创建，先递归创建它）
     *   2. 解析 @Bean 方法的参数依赖
     *   3. 调用 configBean.factoryMethod(args) 创建 Bean
     * </pre>
     *
     * @param definition Bean 的元数据（包含 factoryBeanName 和 factoryMethod）
     * @return 新创建的 Bean 实例
     */
    private Object createFromFactoryMethod(BeanDefinition definition) throws Exception {
        // 获取配置类实例
        Object configBean = singletonMap.get(definition.getFactoryBeanName());
        if (configBean == null) {
            // 配置类还没实例化，先递归创建它
            BeanDefinition configDef = definitionMap.get(definition.getFactoryBeanName());
            configBean = createBeanInstance(configDef);
            singletonMap.put(definition.getFactoryBeanName(), configBean);
        }
    
        Method method = definition.getFactoryMethod();
        method.setAccessible(true); // 允许访问 private 方法
    
        // 解析方法参数的依赖（从容器中查找对应类型的 Bean）
        Object[] args = resolveMethodArguments(method);
        // 调用 factoryMethod 创建 Bean
        return method.invoke(configBean, args);
    }
    
    /**
     * 通过构造器创建 Bean
     * <p>
     * 构造器选择策略：
     * <ol>
     *   <li>优先使用带 @Autowired 的构造器（构造器注入）</li>
     *   <li>否则使用默认无参构造器</li>
     * </ol>
     *
     * @param definition Bean 的元数据
     * @return 新创建的 Bean 实例
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
    
        // 没有 @Autowired 构造器，使用默认无参构造器
        Constructor<?> defaultConstructor = clazz.getDeclaredConstructor();
        defaultConstructor.setAccessible(true);
        return defaultConstructor.newInstance();
    }
    
    /**
     * 解析构造器参数（依赖查找）
     * <p>
     * 遍历构造器的每个参数，按类型从容器中查找对应的 Bean。
     * 例如：{@code public UserService(@Autowired UserRepository repo)}
     * 会查找 UserRepository 类型的 Bean 作为参数。
     *
     * @param constructor 构造器对象
     * @return 参数数组
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
     * 解析 @Bean 方法参数（依赖查找）
     * <p>
     * 与构造器参数解析类似，按类型查找 Bean。
     * 例如：
     * <pre>
     *   @Bean
     *   public UserService userService(UserRepository repo) {
     *       return new UserService(repo); // repo 参数会自动从容器查找
     *   }
     * </pre>
     *
     * @param method @Bean 方法
     * @return 参数数组
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
     * <p>
     * 遍历 singletonMap 中的所有 Bean，调用 populateBean() 注入 @Autowired 和 @Value。
     * 这是 Spring Bean 生命周期中的「属性填充」阶段。
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
     * <p>
     * 扫描 Bean 类及其父类的所有字段，处理：
     * <ul>
     *   <li><b>@Autowired</b> —— 字段注入：从容器中查找对应类型的 Bean 并赋值</li>
     *   <li><b>@Value</b> —— 属性注入：解析占位符表达式并赋值（支持类型转换）</li>
     * </ul>
     * <p>
     * 示例：
     * <pre>
     *   @Component
     *   public class UserService {
     *       @Autowired                              ← 字段注入
     *       private UserRepository userRepository;
     *
     *       @Value("${app.name:MyApp}")              ← 属性注入
     *       private String appName;
     *   }
     * </pre>
     *
     * @param beanName Bean 名称（用于日志输出）
     * @param bean     Bean 实例
     */
    private void populateBean(String beanName, Object bean) throws Exception {
        Class<?> clazz = bean.getClass();

        // 遍历所有字段（包括父类，直到 Object）
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                // 1. 处理 @Autowired 字段注入
                if (field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true); // 允许访问 private 字段
                    // 按字段类型从容器中查找依赖 Bean
                    Object dependency = findBeanByType(field.getType());
                    field.set(bean, dependency); // 反射赋值
                    System.out.println("[MiniSpring] 注入依赖: " + beanName + "." + field.getName()
                            + " <- " + dependency.getClass().getSimpleName());
                }

                // 2. 处理 @Value 属性注入
                if (field.isAnnotationPresent(Value.class)) {
                    field.setAccessible(true);
                    Value valueAnn = field.getAnnotation(Value.class);
                    // 解析占位符表达式，并转换为字段的目标类型
                    Object value = propertyResolver.resolve(valueAnn.value(), field.getType());
                    field.set(bean, value);
                    System.out.println("[MiniSpring] 注入属性: " + beanName + "." + field.getName()
                            + " = " + value);
                }
            }
            // 处理父类字段（支持继承场景）
            clazz = clazz.getSuperclass();
        }
    }

    // ======================== 依赖查找 ========================

    /**
     * 按类型查找 Bean
     * <p>
     * 查找策略（优先级从高到低）：
     * <ol>
     *   <li><b>按名称查找</b> —— 将类型名首字母小写作为 beanName 查找
     *       （例如 UserRepository → "userRepository"）</li>
     *   <li><b>按类型遍历</b> —— 遍历所有 Bean，找第一个类型匹配的</li>
     * </ol>
     *
     * @param requiredType 需要的类型
     * @return 匹配的 Bean 实例
     * @throws RuntimeException 找不到时抛出
     */
    private Object findBeanByType(Class<?> requiredType) {
        // 策略 1：按名称查找（类型名首字母小写 → beanName）
        String typeName = Character.toLowerCase(requiredType.getSimpleName().charAt(0))
                + requiredType.getSimpleName().substring(1);
        Object bean = singletonMap.get(typeName);
        if (bean != null && requiredType.isInstance(bean)) {
            return bean;
        }

        // 策略 2：按类型遍历查找
        for (Object candidate : singletonMap.values()) {
            if (requiredType.isInstance(candidate)) {
                return candidate;
            }
        }

        throw new RuntimeException("未找到类型为 " + requiredType.getName() + " 的 Bean，无法完成注入");
    }

    // ======================== 工具方法 ========================

    /**
     * 获取所有已注册 BeanDefinition 的名称
     *
     * @return Bean 名称列表
     */
    public List<String> getBeanNames() {
        return new ArrayList<>(definitionMap.keySet());
    }

    /**
     * 检查是否包含指定名称的 Bean
     *
     * @param name Bean 名称
     * @return true 如果容器中存在该 Bean
     */
    public boolean containsBean(String name) {
        return singletonMap.containsKey(name);
    }
}
