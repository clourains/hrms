package com.hrms.config;

import com.hrms.annotation.RequireRole;
import com.hrms.util.JwtUtil;
import com.hrms.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * 接口鉴权拦截器
 *
 * 校验请求头里的 JWT，没带或过期一律返回 401。
 * 登录接口在 AuthConfig 里排除，不经过这里。
 * 校验通过的同时把用户名放进 UserContext，Service 层写流水时可以直接取操作人（P009）。
 * P045：接口带 @RequireRole 注解时（方法优先，类兜底），校验 token 中的角色，
 * 不满足返回 HTTP 403；角色取自登录时签发的 token，本系统暂无改角色的入口。
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跨域预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        String token = header;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }

        if (token != null && !token.isEmpty() && jwtUtil.validateToken(token)) {
            String role = jwtUtil.getRoleFromToken(token);

            // P045 角色权限校验
            RequireRole requireRole = resolveRequireRole(handler);
            if (requireRole != null && !hasAnyRole(role, requireRole.value())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json;charset=UTF-8");
                PrintWriter writer = response.getWriter();
                writer.write("{\"code\":403,\"message\":\"没有权限执行此操作\",\"data\":null}");
                writer.flush();
                return false;
            }

            UserContext.set(jwtUtil.getUsernameFromToken(token));
            UserContext.setRole(role);
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write("{\"code\":401,\"message\":\"未登录或登录已过期，请重新登录\",\"data\":null}");
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
        // 线程复用，必须清理，否则会串用户
        UserContext.clear();
    }
}
