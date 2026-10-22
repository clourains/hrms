package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.dto.LoginDTO;
import com.hrms.entity.User;
import com.hrms.mapper.UserMapper;
import com.hrms.service.UserService;
import com.hrms.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户Service实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Map<String, Object> login(LoginDTO loginDTO) {
        // 根据用户名查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        // 密码验证（MD5加密）
        String passwordMd5 = DigestUtils.md5DigestAsHex(loginDTO.getPassword().getBytes());
        if (!passwordMd5.equals(user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 生成token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        // 清除密码
        user.setPassword(null);
        result.put("userInfo", user);

        return result;
    }

    @Override
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
}
