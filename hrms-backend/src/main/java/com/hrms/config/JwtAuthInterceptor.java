package com.hrms.config;

import com.hrms.annotation.RequireRole;
import com.hrms.util.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * @RequireRole 角色校验拦截器（M2-2 后职责收窄）
 *
 * 认证（token 解析、登出黑名单、UserContext 填充）已上移到 Spring Security 的
 * JwtAuthenticationFilter，未认证请求根本到不了这里。
 * P045：接口带 @RequireRole 注解时（方法优先，类兜底），校验用户角色，
 * 不满足返回 HTTP 403；角色取自登录时签发的 token，本系统暂无改角色的入口。
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        RequireRole requireRole = resolveRequireRole(handler);
        if (requireRole == null || hasAnyRole(UserContext.getRole(), requireRole.value())) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write("{\"code\":403,\"message\":\"没有权限执行此操作\",\"data\":null}");
        writer.flush();
        return false;
    }

    /**
     * 优先取方法上的 @RequireRole，没有再看类上的
     */
    private RequireRole resolveRequireRole(Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return null;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        return requireRole;
    }

    private boolean hasAnyRole(String role, String[] allowedRoles) {
        if (role == null) {
            return false;
        }
        for (String allowed : allowedRoles) {
            if (allowed.equals(role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 线程复用，必须清理（JwtAuthenticationFilter 的 finally 也会清，双保险）
        UserContext.clear();
    }
}
