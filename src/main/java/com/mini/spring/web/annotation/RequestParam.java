package com.mini.spring.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 绑定 HTTP 请求参数到方法参数
 * <p>
 * <b>学习要点：</b>
 * 对标 Spring 的 {@code org.springframework.web.bind.annotation.RequestParam}。
 * 用于从 URL 查询字符串中提取参数值，支持必填、默认值、类型转换。
 * <p>
 * <b>使用示例：</b>
 * <pre>
 *   // 请求：GET /search?keyword=spring&page=1
 *   @GetMapping("/search")
 *   public List&lt;Result&gt; search(
 *       @RequestParam String keyword,                    // 必填，缺失则报错
 *       @RequestParam(defaultValue = "1") int page,      // 可选，默认值 1
 *       @RequestParam(value = "size", defaultValue = "10") int pageSize  // 别名 + 默认值
 *   ) { ... }
 * </pre>
 *
 * @see PathVariable 路径变量绑定
 */
@Target(ElementType.PARAMETER)      // 只能标注在方法参数上
@Retention(RetentionPolicy.RUNTIME)  // 运行时保留
public @interface RequestParam {
    /**
     * 请求参数名
     * <p>
     * 默认为空字符串，此时使用参数名匹配。
     * 例如：@RequestParam("userName") 匹配查询参数 userName
     */
    String value() default "";

    /**
     * 是否必填
     * <p>
     * 默认为 true。如果为 true 且参数不存在且无默认值，则抛出异常。
     * 设为 false 表示参数可选。
     */
    boolean required() default true;

    /**
     * 默认值
     * <p>
     * 当请求参数不存在时，使用此默认值。
     * 默认为空字符串（表示无默认值）。
     */
    String defaultValue() default "";
}
