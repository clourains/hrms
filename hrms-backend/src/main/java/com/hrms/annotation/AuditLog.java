package com.hrms.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感操作审计注解（P1-1）
 *
 * 标注在 Controller 写方法上，由 AuditLogAspect 环绕拦截，
 * 自动记录操作人、模块、动作、参数摘要、成功与否、耗时、IP。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * 业务模块名，如：薪资管理
     */
    String module();

    /**
     * 操作类型，如：新增/修改/删除
     */
    String action();
}
