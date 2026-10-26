package com.hrms.config;

import com.hrms.util.JwtUtil;
import com.hrms.util.TokenBlacklist;
import com.hrms.util.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器（M2-2：原 JwtAuthInterceptor 的认证职责上移到 Security 过滤链）
 *
 * 解析请求头里的 JWT：有效则写入 SecurityContext（用户名 + ROLE_* 权限）和
 * UserContext（Service 层写流水取操作人 P009、@RequireRole 拦截器取角色 P045）；
 * 无 token 或非法则保持匿名，由 SecurityConfig 的 authorizeHttpRequests 统一返回 401。
 * P049：登出过的 token 命中黑名单，直接返回 401，退出登录立即失效。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            String token = header;
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7).trim();
            }

            if (token != null && !token.isEmpty() && jwtUtil.validateToken(token)) {
                if (tokenBlacklist.isRevoked(token)) {
                    SecurityConfig.writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "{\"code\":401,\"message\":\"登录已失效，请重新登录\",\"data\":null}");
                    return;
                }

                String role = jwtUtil.getRoleFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);

                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + (role == null ? "user" : role)));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                UserContext.set(username);
                UserContext.setRole(role);
            }

            filterChain.doFilter(request, response);
        } finally {
            // 线程复用，必须清理，否则会串用户
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
