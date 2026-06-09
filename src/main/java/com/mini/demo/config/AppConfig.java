package com.mini.demo.config;

import com.mini.demo.service.UserRepository;
import com.mini.spring.annotation.Bean;
import com.mini.spring.annotation.Configuration;

/**
 * 应用配置类 —— 展示 @Configuration + @Bean 的用法
 * <p>
 * 学习要点：
 * - @Configuration 标记的类相当于 Spring 的 XML 配置文件的 Java 版本
 * - @Bean 方法返回的对象会被注册到 IoC 容器
 * - UserRepository 没有 @Component，但通过 @Bean 方法也能被容器管理
 */
@Configuration
public class AppConfig {

    /**
     * 通过 @Bean 方法注册 UserRepository
     * 相当于 Spring XML 中的 <bean id="userRepository" class="..."/>
     */
    @Bean("userRepository")
    public UserRepository userRepository() {
        System.out.println("[AppConfig] 创建 UserRepository Bean（通过 @Bean 方法）");
        return new UserRepository();
    }
}
