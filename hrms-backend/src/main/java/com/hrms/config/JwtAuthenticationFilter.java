package com.hrms.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.entity.Employee;
import com.hrms.entity.User;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.UserMapper;
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

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private com.hrms.service.OpenApiKeyService openApiKeyService;

    @Autowired
    private com.hrms.service.OpenApiRateLimiter openApiRateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 开放接口不走登录：用请求头 X-API-Key 换一个「admin 身份」，只暴露批量写入路由
            if (request.getRequestURI().startsWith(request.getContextPath() + "/open-api/import")) {
                String apiKey = request.getHeader("X-API-Key");
                if (!openApiKeyService.isValid(apiKey)) {
                    SecurityConfig.writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "{\"code\":401,\"message\":\"X-API-Key 无效，请到系统「开放接口」复制最新密钥\",\"data\":null}");
                    return;
                }
                // 限流兜底：即使密钥泄露，也不能被无限制地批量灌数据。
                // 身份用「密钥 + 真实来源地址」：X-Forwarded-For 可被请求方伪造，
                // 拿它当限流维度等于把限流开关交给调用者，所以这里只信 remoteAddr。
                String clientIdentity = apiKey + "@" + request.getRemoteAddr();
                if (!openApiRateLimiter.allow(clientIdentity)) {
                    response.setHeader("Retry-After", "60");
                    SecurityConfig.writeJson(response, 429,
                            "{\"code\":429,\"message\":\"请求过于频繁，每个来源每分钟最多 60 次，请稍后再试\",\"data\":null}");
                    return;
                }
                UsernamePasswordAuthenticationToken openApiAuth = new UsernamePasswordAuthenticationToken(
                        "open-api", null, List.of(new SimpleGrantedAuthority("ROLE_admin")));
                openApiAuth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(openApiAuth);
                UserContext.set("open-api");
                UserContext.setRole("admin");
                filterChain.doFilter(request, response);
                return;
            }

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
                fillDeptId(username, role);
            }

            filterChain.doFilter(request, response);
        } finally {
            // 线程复用，必须清理，否则会串用户
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * P4-3 部门数据权限：按 token 用户名解析绑定员工的部门ID，
     * 供 Service 层对 manager 角色做本部门数据过滤；admin 不限流不查询。
     */
    private void fillDeptId(String username, String role) {
        if (username == null || "admin".equals(role)) {
            return;
        }
        try {
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            if (user != null && user.getEmpId() != null) {
                Employee emp = employeeMapper.selectById(user.getEmpId());
                if (emp != null) {
                    UserContext.setDeptId(emp.getDeptId());
                }
            }
        } catch (Exception ignored) {
            // 部门解析失败不影响认证主流程
        }
    }
}
