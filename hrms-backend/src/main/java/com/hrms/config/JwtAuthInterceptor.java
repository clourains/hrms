package com.hrms.config;

import com.hrms.util.JwtUtil;
import com.hrms.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
            UserContext.set(jwtUtil.getUsernameFromToken(token));
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

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 线程复用，必须清理，否则会串用户
        UserContext.clear();
    }
}
