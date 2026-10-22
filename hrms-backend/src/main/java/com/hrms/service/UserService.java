package com.hrms.service;

import com.hrms.dto.LoginDTO;
import com.hrms.entity.User;

import java.util.Map;

/**
 * 用户Service接口
 */
public interface UserService {

    /**
     * 登录验证
     *
     * @param loginDTO 登录信息
     * @return 包含token和用户信息的Map
     */
    Map<String, Object> login(LoginDTO loginDTO);

    /**
     * 根据ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    User getById(Long id);
}
