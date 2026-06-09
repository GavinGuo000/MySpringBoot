package com.mini.spring.core;

import java.lang.reflect.Method;

/**
 * Bean 定义 —— 描述一个 Bean 的元信息
 * <p>
 * 学习要点：BeanDefinition 是 Spring 的核心抽象之一，
 * 它把"如何创建一个 Bean"的信息封装起来，供容器使用。
 */
public class BeanDefinition {

    /**
     * Bean 的类型
     */
    private final Class<?> beanClass;

    /**
     * Bean 的名称
     */
    private final String beanName;

    /**
     * 如果是通过 @Bean 方法创建的，记录工厂对象名和方法
     */
    private String factoryBeanName;
    private Method factoryMethod;

    public BeanDefinition(Class<?> beanClass, String beanName) {
        this.beanClass = beanClass;
        this.beanName = beanName;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public String getBeanName() {
        return beanName;
    }

    public String getFactoryBeanName() {
        return factoryBeanName;
    }

    public void setFactoryBeanName(String factoryBeanName) {
        this.factoryBeanName = factoryBeanName;
    }

    public Method getFactoryMethod() {
        return factoryMethod;
    }

    public void setFactoryMethod(Method factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    /**
     * 是否通过工厂方法创建
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
