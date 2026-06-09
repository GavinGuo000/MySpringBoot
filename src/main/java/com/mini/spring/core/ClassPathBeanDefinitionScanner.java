package com.mini.spring.core;

import com.mini.spring.annotation.Component;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 类路径 Bean 定义扫描器 —— 扫描指定包下所有带 @Component（及其派生注解）的类
 * <p>
 * <b>学习要点：</b>
 * 这是 Spring 的 {@code @ComponentScan} 机制的简化实现，对标 Spring 框架中的
 * {@code org.springframework.context.annotation.ClassPathBeanDefinitionScanner}。
 * <p>
 * Spring 是如何找到所有需要管理的 Bean 的？答案就是「类路径扫描」：
 * <ol>
 *   <li>将包名转为目录路径（如 com.mini.demo → com/mini/demo）</li>
 *   <li>通过 ClassLoader 获取该目录在文件系统中的位置</li>
 *   <li>递归遍历目录下所有 .class 文件</li>
 *   <li>通过 Class.forName() 加载每个类</li>
 *   <li>检查类上是否有 @Component 或其派生注解（如 @RestController、@Configuration）</li>
 *   <li>将符合条件的类封装为 BeanDefinition 返回</li>
 * </ol>
 * <p>
 * <b>派生注解识别原理：</b>
 * <pre>
 *   @Component                    ← 元注解
 *   public @interface RestController {}
 *
 *   @RestController               ← 标注了 @RestController 的类
 *   public class UserController {}
 *
 *   扫描时：先检查 UserController 是否有 @Component（没有），
 *   再检查 @RestController 注解本身是否有 @Component（有！）→ 识别为组件
 * </pre>
 *
 * @see BeanDefinition 扫描结果封装为 BeanDefinition
 * @see Component 元注解，所有派生注解的基础
 */
public class ClassPathBeanDefinitionScanner {

    /**
     * 扫描指定包下所有组件类，生成 BeanDefinition 列表
     * <p>
     * 这是扫描的主入口方法，流程如下：
     * <pre>
     *   basePackages ("com.mini.demo")
     *       ↓
     *   findCandidateClasses() —— 找到包下所有 .class 文件对应的 Class 对象
     *       ↓
     *   isComponent() —— 判断每个类是否是组件（带 @Component 或派生注解）
     *       ↓
     *   resolveBeanName() —— 确定 Bean 名称
     *       ↓
     *   new BeanDefinition() —— 封装为 BeanDefinition 返回
     * </pre>
     *
     * @param basePackages 要扫描的包名（支持多个包）
     * @return BeanDefinition 列表，每个元素代表一个扫描到的组件
     */
    public List<BeanDefinition> scan(String... basePackages) {
        List<BeanDefinition> definitions = new ArrayList<>();

        for (String basePackage : basePackages) {
            // 步骤 1：找到包下所有的 Class 对象
            List<Class<?>> classes = findCandidateClasses(basePackage);
            for (Class<?> clazz : classes) {
                // 步骤 2：判断是否是组件（带 @Component 或派生注解）
                if (isComponent(clazz)) {
                    // 步骤 3：解析 Bean 名称
                    String beanName = resolveBeanName(clazz);
                    // 步骤 4：创建 BeanDefinition 并加入列表
                    BeanDefinition bd = new BeanDefinition(clazz, beanName);
                    definitions.add(bd);
                    System.out.println("[MiniSpring] 扫描到组件: " + beanName + " -> " + clazz.getName());
                }
            }
        }

        return definitions;
    }

    /**
     * 判断类是否是组件（带有 @Component 或其派生注解）
     * <p>
     * 检查逻辑分两层：
     * <ol>
     *   <li>直接检查：类上是否有 @Component 注解</li>
     *   <li>间接检查：类上的任意注解是否本身被 @Component 标注（派生注解）</li>
     * </ol>
     * <p>
     * 派生注解示例：
     * <ul>
     *   <li>@RestController = @Component + @ResponseBody</li>
     *   <li>@Configuration = @Component（语义上）</li>
     *   <li>@Service = @Component</li>
     * </ul>
     *
     * @param clazz 要检查的类
     * @return true 如果是组件
     */
    private boolean isComponent(Class<?> clazz) {
        // 第一层：直接标注 @Component
        if (clazz.isAnnotationPresent(Component.class)) {
            return true;
        }
        // 第二层：检查是否有派生注解（注解上标注了 @Component）
        // 遍历类上的所有注解，看这些注解的类型本身是否被 @Component 标注
        for (Annotation ann : clazz.getAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(Component.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析 Bean 名称
     * <p>
     * 命名优先级：
     * <ol>
     *   <li>@Component("myService") 指定的名称</li>
     *   <li>派生注解如 @RestController("myCtrl") 指定的名称</li>
     *   <li>默认规则：类名首字母小写（如 UserService → "userService"）</li>
     * </ol>
     *
     * @param clazz 组件类
     * @return Bean 名称
     */
    private String resolveBeanName(Class<?> clazz) {
        // 优先检查 @Component 的 value 属性
        Component comp = clazz.getAnnotation(Component.class);
        if (comp != null && !comp.value().isEmpty()) {
            return comp.value();
        }
        // 检查派生注解的 value（如 @RestController("myCtrl")）
        for (Annotation ann : clazz.getAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(Component.class)) {
                try {
                    // 通过反射获取注解的 value() 方法值
                    String value = (String) ann.annotationType().getMethod("value").invoke(ann);
                    if (value != null && !value.isEmpty()) {
                        return value;
                    }
                } catch (Exception ignored) {
                    // 注解没有 value() 方法，忽略
                }
            }
        }
        // 默认规则：类名首字母小写
        return Character.toLowerCase(clazz.getSimpleName().charAt(0)) + clazz.getSimpleName().substring(1);
    }

    /**
     * 扫描包下所有的类
     * <p>
     * 核心流程：
     * <pre>
     *   "com.mini.demo" → "com/mini/demo"
     *       ↓
     *   ClassLoader.getResources("com/mini/demo")
     *       ↓ 获取该包在文件系统中的实际目录
     *   File: /path/to/target/classes/com/mini/demo
     *       ↓
     *   scanDirectory() —— 递归遍历目录，加载所有 .class 文件
     * </pre>
     *
     * @param basePackage 包名
     * @return 该包下所有 Class 对象的列表
     */
    private List<Class<?>> findCandidateClasses(String basePackage) {
        List<Class<?>> classes = new ArrayList<>();
        // 将包名转换为目录路径（Java 包分隔符 . → 文件路径分隔符 /）
        String path = basePackage.replace('.', '/');

        try {
            // 通过 ClassLoader 获取包路径对应的所有资源 URL
            // （可能有多个，比如 src/main/classes 和 jar 包中都有）
            Enumeration<URL> resources = getClass().getClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                // 只处理文件系统上的目录（不处理 jar 包内的资源）
                if ("file".equals(resource.getProtocol())) {
                    File directory = new File(resource.getFile());
                    scanDirectory(directory, basePackage, classes);
                }
            }
        } catch (IOException e) {
            System.err.println("[MiniSpring] 扫描包失败: " + basePackage + " - " + e.getMessage());
        }

        return classes;
    }

    /**
     * 递归扫描目录下的所有 .class 文件
     * <p>
     * 深度优先遍历目录树，对每个 .class 文件通过 Class.forName() 加载为 Class 对象。
     * <p>
     * 示例目录结构：
     * <pre>
     *   com/mini/demo/
     *   ├── MySpringBootApplication.class  ← 加载
     *   ├── controller/
     *   │   └── HelloController.class      ← 加载（递归进入子目录）
     *   └── service/
     *       ├── UserService.class           ← 加载
     *       └── UserRepository.class        ← 加载
     * </pre>
     *
     * @param directory 当前扫描的目录
     * @param packageName 当前目录对应的包名
     * @param classes     收集结果的列表
     */
    private void scanDirectory(File directory, String packageName, List<Class<?>> classes) {
        if (!directory.exists()) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // 子目录：递归扫描，包名拼接子目录名
                scanDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                // .class 文件：拼接完整类名并加载
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    // Class.forName() 会触发类的加载和初始化
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    System.err.println("[MiniSpring] 无法加载类: " + className);
                }
            }
        }
    }
}
