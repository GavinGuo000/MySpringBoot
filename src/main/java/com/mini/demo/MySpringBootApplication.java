package com.mini.demo;

import com.mini.spring.boot.MySpringApplication;

/**
 * Mini Spring Boot 示例应用 - 启动入口
 * <p>
 * 这是一个完整的 Mini Spring Boot 应用，展示了以下核心功能：
 * 1. IoC 容器 & 依赖注入 (@Component, @Autowired)
 * 2. 配置类 & @Bean 方法 (@Configuration, @Bean)
 * 3. 属性注入 (@Value)
 * 4. REST 控制器 (@RestController, @GetMapping, @PostMapping)
 * 5. 内嵌 Web 服务器
 */
public class MySpringBootApplication {

    public static void main(String[] args) {
        MySpringApplication.run(MySpringBootApplication.class, args);
    }
}
