package com.mini.spring.web.server;

import java.util.ArrayList;
import java.util.List;

/**
 * 拦截器注册表 —— 对标 Spring MVC 的 InterceptorRegistry
 * <p>
 * <b>学习要点：</b>
 * 在 Spring MVC 中，通过 {@code WebMvcConfigurer.addInterceptors()} 注册拦截器，
 * 并可以指定拦截器作用的 URL 路径模式。这里简化实现相同的功能。
 * <p>
 * <b>使用方式（两种注册方式）：</b>
 * <p>
 * <b>方式 1：实现 HandlerInterceptor 接口 + @Component（自动注册）</b>
 * <pre>
 *   @Component
 *   public class AuthInterceptor implements HandlerInterceptor {
 *       public boolean preHandle(HttpExchange exchange, Object handler) {
 *           // 检查 Token
 *           return true;
 *       }
 *   }
 * </pre>
 * <p>
 * <b>方式 2：通过 InterceptorRegistry 手动注册（支持路径过滤）</b>
 * <pre>
 *   @Bean
 *   public InterceptorRegistry interceptorRegistry() {
 *       InterceptorRegistry registry = new InterceptorRegistry();
 *       registry.addInterceptor(new AuthInterceptor())
 *               .addPathPatterns("/api/**")
 *               .excludePathPatterns("/api/login");
 *       return registry;
 *   }
 * </pre>
 *
 * @see HandlerInterceptor 拦截器接口
 */
public class InterceptorRegistry {

    /** 已注册的拦截器映射列表 */
    private final List<InterceptorMapping> mappings = new ArrayList<>();

    /**
     * 添加拦截器
     * <p>
     * 对标 Spring MVC 的 {@code InterceptorRegistry.addInterceptor()}。
     * 返回 {@link InterceptorMapping} 对象，可以链式配置路径过滤规则。
     *
     * @param interceptor 拦截器实例
     * @return 拦截器映射配置对象（支持链式调用）
     */
    public InterceptorMapping addInterceptor(HandlerInterceptor interceptor) {
        InterceptorMapping mapping = new InterceptorMapping(interceptor);
        mappings.add(mapping);
        return mapping;
    }

    /**
     * 获取所有已注册的拦截器映射
     *
     * @return 拦截器映射列表
     */
    public List<InterceptorMapping> getMappings() {
        return mappings;
    }

    /**
     * 拦截器映射 —— 封装拦截器及其路径过滤规则
     * <p>
     * 对标 Spring MVC 的 {@code MappedInterceptor}。
     * 支持链式配置 include/exclude 路径模式。
     */
    public static class InterceptorMapping {

        /** 拦截器实例 */
        private final HandlerInterceptor interceptor;

        /** 拦截的路径模式（如 "/api/**"） */
        private String[] includePatterns = {};

        /** 排除的路径模式（如 "/api/login"） */
        private String[] excludePatterns = {};

        /** 执行顺序（数值越小越先执行） */
        private int order = 0;

        public InterceptorMapping(HandlerInterceptor interceptor) {
            this.interceptor = interceptor;
        }

        /**
         * 指定拦截的路径模式
         * <p>
         * 对标 Spring MVC 的 {@code InterceptorRegistration.addPathPatterns()}。
         * 支持以下匹配规则：
         * <ul>
         *   <li>"*" 或 "/**" —— 匹配所有路径</li>
         *   <li>"/api/**" —— 匹配以 /api/ 开头的所有路径</li>
         *   <li>"/api/users" —— 精确匹配</li>
         * </ul>
         *
         * @param patterns 路径模式
         * @return 自身（支持链式调用）
         */
        public InterceptorMapping addPathPatterns(String... patterns) {
            this.includePatterns = patterns;
            return this;
        }

        /**
         * 指定排除的路径模式（匹配的路径不会被拦截）
         * <p>
         * 对标 Spring MVC 的 {@code InterceptorRegistration.excludePathPatterns()}。
         *
         * @param patterns 排除的路径模式
         * @return 自身（支持链式调用）
         */
        public InterceptorMapping excludePathPatterns(String... patterns) {
            this.excludePatterns = patterns;
            return this;
        }

        /**
         * 设置执行顺序
         *
         * @param order 顺序值（数值越小越先执行）
         * @return 自身（支持链式调用）
         */
        public InterceptorMapping setOrder(int order) {
            this.order = order;
            return this;
        }

        /**
         * 判断给定路径是否应被此拦截器拦截
         * <p>
         * 逻辑：
         * <ol>
         *   <li>如果路径匹配排除模式 → 不拦截</li>
         *   <li>如果未配置 include 模式 → 拦截所有</li>
         *   <li>如果路径匹配 include 模式 → 拦截</li>
         * </ol>
         *
         * @param path 请求路径
         * @return 是否应拦截
         */
        public boolean shouldIntercept(String path) {
            // 先检查排除规则
            if (matchesAny(path, excludePatterns)) return false;
            // 未配置 include 规则时，拦截所有
            if (includePatterns.length == 0) return true;
            // 检查 include 规则
            return matchesAny(path, includePatterns);
        }

        public HandlerInterceptor getInterceptor() {
            return interceptor;
        }

        public int getOrder() {
            return order;
        }

        /**
         * 检查路径是否匹配任一模式
         */
        private boolean matchesAny(String path, String[] patterns) {
            for (String pattern : patterns) {
                if (matchesPattern(path, pattern)) return true;
            }
            return false;
        }

        /**
         * 路径匹配规则
         * <ul>
         *   <li>"*" 或 "/**" —— 匹配所有</li>
         *   <li>"/api/**" —— 前缀匹配（/api/ 开头的任意路径）</li>
         *   <li>"/api/users" —— 精确匹配</li>
         * </ul>
         */
        private boolean matchesPattern(String path, String pattern) {
            if (pattern.equals("*") || pattern.equals("/**")) return true;
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                return path.startsWith(prefix);
            }
            if (pattern.endsWith("/*")) {
                String prefix = pattern.substring(0, pattern.length() - 2);
                return path.startsWith(prefix);
            }
            return pattern.equals(path);
        }
    }
}
