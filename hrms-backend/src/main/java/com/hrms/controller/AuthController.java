package com.hrms.controller;

import com.hrms.dto.LoginDTO;
import com.hrms.entity.User;
import com.hrms.service.UserService;
import com.hrms.util.JwtUtil;
import com.hrms.util.Result;
import com.hrms.util.TokenBlacklist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 认证Controller
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        try {
            // P5-2：携带客户端 IP/UA，登录日志可追溯来源
            Map<String, Object> result = userService.login(loginDTO, getClientIp(request),
                    request.getHeader("User-Agent"));
            return Result.success("登录成功", result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取客户端真实 IP（经反向代理时优先 X-Forwarded-For 首段）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info")
    public Result<User> info(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                return Result.error(401, "未授权");
            }
            User user = userService.getById(userId);
            return Result.success(user);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改密码：校验原密码后更新为 BCrypt 新密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                return Result.error(401, "未授权");
            }
            userService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
            return Result.success("密码修改成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 退出登录（P049）：把当前 token 登记进黑名单，拦截器校验时立即失效
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        if (token != null && !token.isEmpty()) {
            tokenBlacklist.revoke(token);
        }
        return Result.success("退出成功", null);
    }
}
