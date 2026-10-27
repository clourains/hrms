package com.hrms.util;

/**
 * 当前登录用户上下文
 *
 * JwtAuthInterceptor 在 preHandle 里把 token 中的用户名放进来，请求结束时清掉。
 * Service 层可以拿到"谁在操作"，用于写库存流水的操作人（P009）。
 */
public class UserContext {

    private static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();

    /**
     * 当前登录用户的角色（P045），如 admin / user / manager
     */
    private static final ThreadLocal<String> CURRENT_ROLE = new ThreadLocal<>();

    /**
     * 当前登录人绑定员工的部门ID（P4-3 部门数据权限），
     * 由 JwtAuthenticationFilter 登录解析时填充，manager 角色按此过滤数据范围
     */
    private static final ThreadLocal<Long> CURRENT_DEPT_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(String username) {
        CURRENT_USER.set(username);
    }

    public static String get() {
        return CURRENT_USER.get();
    }

    public static void setRole(String role) {
        CURRENT_ROLE.set(role);
    }

    public static String getRole() {
        return CURRENT_ROLE.get();
    }

    /**
     * 当前用户是否管理员
     */
    public static boolean isAdmin() {
        return "admin".equals(CURRENT_ROLE.get());
    }

    /**
     * 当前用户是否部门经理（P4-3）
     */
    public static boolean isManager() {
        return "manager".equals(CURRENT_ROLE.get());
    }

    public static void setDeptId(Long deptId) {
        CURRENT_DEPT_ID.set(deptId);
    }

    public static Long getDeptId() {
        return CURRENT_DEPT_ID.get();
    }

    /**
     * 取当前用户，取不到时给个兜底值，避免流水里出现空白
     */
    public static String getOrDefault(String defaultValue) {
        String user = CURRENT_USER.get();
        return (user == null || user.trim().isEmpty()) ? defaultValue : user;
    }

    public static void clear() {
        CURRENT_USER.remove();
        CURRENT_ROLE.remove();
        CURRENT_DEPT_ID.remove();
    }
}
