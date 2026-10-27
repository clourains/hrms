package com.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.LoginDTO;
import com.hrms.entity.User;

import java.util.Map;

/**
 * 用户Service接口
 */
public interface UserService {

    /**
     * 登录验证（P5-2：携带客户端信息写登录日志）
     *
     * @param loginDTO  登录信息
     * @param ip        客户端IP
     * @param userAgent 浏览器UA
     * @return 包含token和用户信息的Map
     */
    Map<String, Object> login(LoginDTO loginDTO, String ip, String userAgent);

    /**
     * 根据ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    User getById(Long id);

    /**
     * 分页查询用户（P5-1 用户管理）
     */
    Page<User> pageQuery(long current, long size, String keyword, String role);

    /**
     * 新建用户：用户名唯一校验，密码 BCrypt 编码，默认 123456
     */
    User createUser(User user);

    /**
     * 更新用户（仅昵称/角色/绑定员工/状态），禁止禁用或降级当前登录的管理员账号
     *
     * @param operator 操作人用户名
     */
    void updateUser(User user, String operator);

    /**
     * 逻辑删除用户，禁止删除当前登录账号
     *
     * @param operator 操作人用户名
     */
    void deleteUser(Long id, String operator);

    /**
     * 重置密码为指定值（BCrypt 编码）
     */
    void resetPassword(Long id, String newPassword);

    /**
     * 当前登录人修改密码：校验原密码后写入 BCrypt 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}
