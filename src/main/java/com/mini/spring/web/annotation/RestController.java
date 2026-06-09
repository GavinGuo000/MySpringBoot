package com.mini.spring.web.annotation;

import com.mini.spring.annotation.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个类为 REST 控制器，自动序列化为 JSON
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component  // 控制器本身也是组件
public @interface RestController {
    String value() default "";
}
