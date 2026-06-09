package com.mini.spring.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 绑定 HTTP 请求参数到方法参数
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParam {
    /**
     * 请求参数名
     */
    String value() default "";

    /**
     * 是否必填
     */
    boolean required() default true;

    /**
     * 默认值
     */
    String defaultValue() default "";
}
