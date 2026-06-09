package com.mini.spring.web.server;

/**
 * 过滤器注册 Bean —— 对标 Spring Boot 的 FilterRegistrationBean
 * <p>
 * <b>学习要点：</b>
 * 在 Spring Boot 中，可以通过 {@code FilterRegistrationBean} 来注册 Filter，
 * 并指定过滤器作用的 URL 路径模式。这里简化实现，支持路径匹配。
 * <p>
 * <b>使用示例：</b>
 * <pre>
 *   @Component
 *   public class MyFilter implements Filter {
 *       public void doFilter(HttpExchange exchange, FilterChain chain) throws IOException {
 *           System.out.println("过滤请求: " + exchange.getRequestURI());
 *           chain.doFilter(exchange);
 *       }
 *   }
 * </pre>
 * <p>
 * 也可以直接在 {@code @Configuration} 中用 {@code @Bean} 注册：
 * <pre>
 *   @Bean
 *   public FilterRegistrationBean myFilterRegistration() {
 *       FilterRegistrationBean registration = new FilterRegistrationBean();
 *       registration.setFilter(new MyFilter());
 *       registration.addUrlPatterns("/api/*");
 *       registration.setOrder(1);
 *       return registration;
 *   }
 * </pre>
 *
 * @see Filter 过滤器接口
 */
public class FilterRegistrationBean {

    /** 要注册的过滤器实例 */
    private Filter filter;

    /** 过滤器作用的 URL 路径模式（如 "/api/*"），为空则匹配所有路径 */
    private String[] urlPatterns = {};

    /** 过滤器执行顺序（数值越小越先执行） */
    private int order = 0;

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public String[] getUrlPatterns() {
        return urlPatterns;
    }

    public void addUrlPatterns(String... patterns) {
        this.urlPatterns = patterns;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * 判断给定路径是否匹配此过滤器的 URL 模式
     * <p>
     * 支持以下匹配规则：
     * <ul>
     *   <li>空模式或 "*" —— 匹配所有路径</li>
     *   <li>"/api/*" —— 匹配以 /api/ 开头的所有路径</li>
     *   <li>"/api/users" —— 精确匹配</li>
     * </ul>
     *
     * @param path 请求路径
     * @return 是否匹配
     */
    public boolean matches(String path) {
        if (urlPatterns == null || urlPatterns.length == 0) return true;
        for (String pattern : urlPatterns) {
            if (pattern.equals("*") || pattern.equals("/*")) return true;
            if (pattern.endsWith("/*")) {
                // 前缀匹配："/api/*" → 匹配 "/api/" 开头的所有路径
                String prefix = pattern.substring(0, pattern.length() - 2);
                if (path.startsWith(prefix)) return true;
            } else if (pattern.equals(path)) {
                // 精确匹配
                return true;
            }
        }
        return false;
    }
}
