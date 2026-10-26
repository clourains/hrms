package com.hrms.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.PrintWriter;
import java.util.List;

/**
 * Spring Security 6 配置（M2-2）
 *
 * 认证：JwtAuthenticationFilter 解析 token（含登出黑名单 P049），
 *       校验通过后写入 SecurityContext（角色转 ROLE_* authority）和 UserContext。
 * 授权：除登录/错误页外全部要求已认证；@RequireRole 的细粒度角色校验
 *       仍由 JwtAuthInterceptor 在 MVC 层完成（方法优先，类兜底）。
 * CORS：统一收口到 Security 的 CorsFilter（原 MVC CorsConfig 已移除），
 *       保证 401/403 响应也带跨域头，前端能读到错误信息。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(SecurityConfig::writeUnauthorized)
                        .accessDeniedHandler(SecurityConfig::writeForbidden))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 未认证（没带 token / token 过期 / token 非法）：与原拦截器的 401 响应保持一致 */
    private static void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws java.io.IOException {
        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "{\"code\":401,\"message\":\"未登录或登录已过期，请重新登录\",\"data\":null}");
    }

    /** 已认证但无权访问：与原拦截器的 403 响应保持一致 */
    private static void writeForbidden(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws java.io.IOException {
        writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                "{\"code\":403,\"message\":\"没有权限执行此操作\",\"data\":null}");
    }

    static void writeJson(HttpServletResponse response, int status, String body) throws java.io.IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write(body);
        writer.flush();
    }
}
