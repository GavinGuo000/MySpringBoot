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
 * 学习要点：Spring 的组件扫描机制（@ComponentScan）的简化实现，
 * 理解 Spring 是如何找到所有需要管理的 Bean 的。
 */
public class ClassPathBeanDefinitionScanner {

    /**
     * 扫描指定包下所有组件类，生成 BeanDefinition
     *
     * @param basePackages 要扫描的包
     * @return BeanDefinition 列表
     */
    public List<BeanDefinition> scan(String... basePackages) {
        List<BeanDefinition> definitions = new ArrayList<>();

        for (String basePackage : basePackages) {
            List<Class<?>> classes = findCandidateClasses(basePackage);
            for (Class<?> clazz : classes) {
                if (isComponent(clazz)) {
                    String beanName = resolveBeanName(clazz);
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
     */
    private boolean isComponent(Class<?> clazz) {
        // 直接标注 @Component
        if (clazz.isAnnotationPresent(Component.class)) {
            return true;
        }
        // 检查是否有派生注解（注解上标注了 @Component）
        for (Annotation ann : clazz.getAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(Component.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析 Bean 名称：优先使用注解中指定的名称，否则类名首字母小写
     */
    private String resolveBeanName(Class<?> clazz) {
        // 检查 @Component 的 value
        Component comp = clazz.getAnnotation(Component.class);
        if (comp != null && !comp.value().isEmpty()) {
            return comp.value();
        }
        // 检查派生注解的 value（如 @RestController("myCtrl")）
        for (Annotation ann : clazz.getAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(Component.class)) {
                try {
                    String value = (String) ann.annotationType().getMethod("value").invoke(ann);
                    if (value != null && !value.isEmpty()) {
                        return value;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        // 默认：类名首字母小写
        return Character.toLowerCase(clazz.getSimpleName().charAt(0)) + clazz.getSimpleName().substring(1);
    }

    /**
     * 扫描包下所有的类
     */
    private List<Class<?>> findCandidateClasses(String basePackage) {
        List<Class<?>> classes = new ArrayList<>();
        String path = basePackage.replace('.', '/');

        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
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
     */
    private void scanDirectory(File directory, String packageName, List<Class<?>> classes) {
        if (!directory.exists()) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    System.err.println("[MiniSpring] 无法加载类: " + className);
                }
            }
        }
    }
}
