# Spring Boot

> 100% 纯 Java 原生实现，无任何 Spring 依赖，适合深入学习 Spring Boot 核心原理。

---

## 这个项目是什么？

这是一个可以**直接运行的 Spring Boot**，完整实现了 Spring Boot 的核心机制：

| 功能 | 说明 | 对应 Spring 概念 |
|------|------|------------------|
| `@Component` `@Autowired` `@Value` | 注解驱动的依赖注入 | Spring IoC 容器 |
| `@Configuration` `@Bean` | Java 配置类 | Spring JavaConfig |
| `@RestController` `@GetMapping` | RESTful 路由 | Spring MVC |
| `@PathVariable` `@RequestParam` | 参数自动绑定 | Spring MVC 参数解析 |
| 内嵌 HTTP 服务器 | 一键启动 Web 服务 | 内嵌 Tomcat |
| `application.properties` | 配置文件加载 | Spring Environment |

---

## 快速开始

### 环境要求

- Java 17+
- Maven 3.x

### 运行项目

```bash
# 1. 进入项目目录
cd MySpringBoot

# 2. 编译并运行
mvn compile exec:java -Dexec.mainClass="com.mini.demo.MySpringBootApplication"
```

启动成功后会看到：

```
[MiniSpring] =============================================
[MiniSpring]      Mini Spring Boot 启动中...
[MiniSpring] =============================================
[MiniSpring] 扫描到组件: greetingService -> ...
[MiniSpring] 扫描到组件: userService -> ...
[MiniSpring] 注册路由: GET /api/ -> HelloController#index
[MiniSpring] 注册路由: GET /api/hello -> HelloController#hello
...
[MiniSpring]   🚀 Mini Spring Boot 启动成功!
[MiniSpring]   📍 地址: http://localhost:8080
```

### 测试 API

```bash
# 首页
curl http://localhost:8080/api/

# 问候接口（查询参数）
curl http://localhost:8080/api/hello?name=Spring

# 应用信息（@Value 属性注入）
curl http://localhost:8080/api/info

# 用户列表（@Autowired 依赖注入链）
curl http://localhost:8080/api/users

# 用户详情（路径变量）
curl http://localhost:8080/api/users/1

# 创建用户（POST 请求）
curl -X POST "http://localhost:8080/api/users/create?name=赵六&email=test@example.com"
```

---

## 项目结构

```
src/main/java/com/mini/
├── spring/                    # ← 框架核心代码（你要学习的部分）
│   ├── annotation/            # 核心注解定义
│   │   ├── Component.java     # @Component 组件标记
│   │   ├── Autowired.java     # @Autowired 依赖注入
│   │   ├── Configuration.java # @Configuration 配置类
│   │   ├── Bean.java          # @Bean 方法级 Bean 定义
│   │   └── Value.java         # @Value 属性注入
│   ├── web/annotation/        # Web 层注解
│   │   ├── RestController.java
│   │   ├── RequestMapping.java
│   │   ├── GetMapping.java
│   │   ├── PostMapping.java
│   │   ├── PathVariable.java
│   │   └── RequestParam.java
│   ├── core/                  # IoC 容器核心
│   │   ├── BeanDefinition.java           # Bean 元信息
│   │   ├── ClassPathBeanDefinitionScanner.java  # 包扫描
│   │   ├── BeanFactory.java              # IoC 容器
│   │   ├── PropertyResolver.java         # 属性解析
│   │   └── AnnotationConfigApplicationContext.java  # 应用上下文
│   ├── web/server/            # Web 服务器
│   │   ├── HandlerMapping.java           # 路由映射
│   │   ├── DispatcherHandler.java        # 请求分发
│   │   └── EmbeddedWebServer.java        # 内嵌服务器
│   └── boot/
│       └── MySpringApplication.java      # 一键启动入口
│
└── demo/                      # ← 示例应用（用框架写业务代码）
    ├── MySpringBootApplication.java   # 应用入口
    ├── config/AppConfig.java          # 配置类示例
    ├── controller/HelloController.java # 控制器示例
    └── service/                       # 服务层示例
        ├── GreetingService.java
        ├── UserService.java
        └── UserRepository.java
```

---

## 学习路线（建议顺序）

### 第一步：理解注解定义

阅读 `spring/annotation/` 下的注解类，理解：
- 注解本身只是**标记**，真正的逻辑在框架处理时
- `@Configuration` 上标了 `@Component`，理解**派生注解**机制

### 第二步：理解 IoC 容器（核心）

按顺序阅读 `spring/core/` 下的类：

1. **`BeanDefinition`** — 每个 Bean 的"说明书"
2. **`ClassPathBeanDefinitionScanner`** — 如何扫描找到所有组件
3. **`BeanFactory`** — IoC 容器的核心，三步流程：注册 → 实例化 → 注入
4. **`PropertyResolver`** — 如何加载 `application.properties` 并解析 `${}` 占位符
5. **`AnnotationConfigApplicationContext`** — 把上面所有组件组装起来

### 第三步：理解 Web 层

阅读 `spring/web/server/` 下的类：

1. **`HandlerMapping`** — 如何把 URL 映射到方法
2. **`DispatcherHandler`** — 如何调用方法并返回 JSON
3. **`EmbeddedWebServer`** — 如何启动一个 HTTP 服务器

### 第四步：对照示例应用

阅读 `demo/` 下的代码，验证你学到了什么：
- `@Component` + `@Value` → `GreetingService`
- `@Autowired` 链式注入 → `UserService → UserRepository`
- `@Configuration` + `@Bean` → `AppConfig`
- `@RestController` + 各种注解 → `HelloController`

---

## 核心知识点速查

| 你想了解... | 看哪个文件 |
|---|---|
| Spring 如何找到所有 Bean？ | `ClassPathBeanDefinitionScanner.java` |
| IoC 容器怎么创建对象？ | `BeanFactory.java` → `instantiateBeans()` |
| @Autowired 怎么注入依赖？ | `BeanFactory.java` → `populateBeans()` |
| @Configuration + @Bean 怎么工作？ | `BeanFactory.java` → `processBeanMethods()` |
| @Value 怎么读取配置？ | `PropertyResolver.java` |
| URL 怎么路由到方法？ | `HandlerMapping.java` |
| 请求参数怎么绑定到方法参数？ | `DispatcherHandler.java` → `resolveMethodArguments()` |
| Spring Boot 启动流程是什么？ | `MySpringApplication.java` |

---

## 扩展练习

学完后可以尝试自己添加这些功能：

- [ ] 实现 `@Service` `@Repository` 注解
- [ ] 实现构造器注入（`@Autowired` 标在构造器上）
- [ ] 添加拦截器 / 过滤器机制
- [ ] 支持 `@RequestBody` 接收 JSON 请求体
- [ ] 实现简单的 AOP（动态代理）
- [ ] 实现 Bean 生命周期回调（`@PostConstruct`）
