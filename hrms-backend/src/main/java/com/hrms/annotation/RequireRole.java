package com.hrms.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色控制注解（P045 角色权限生效）
 *
 * 标注在 Controller 方法或类上，要求当前登录用户具备指定角色之一才能访问。
 * 方法上的注解优先于类上的注解；未标注的接口保持"登录即可访问"。
 *
 * JwtAuthInterceptor 在 preHandle 中读取该注解并校验 token 里的 role，
 * 不满足时返回 HTTP 403，前端提示"没有权限"但不登出。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * 允许访问的角色列表，任一匹配即可，如 {"admin"}
     */
    String[] value();
}
