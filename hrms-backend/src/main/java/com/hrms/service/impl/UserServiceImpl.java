package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.dto.LoginDTO;
import com.hrms.entity.User;
import com.hrms.mapper.UserMapper;
import com.hrms.service.UserService;
import com.hrms.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户Service实现类
 */
@Service
public class UserServiceImpl implements UserService {

    /**
     * BCrypt 加盐编码器（P046）。默认强度 10，每次 encode 自动生成随机盐，
     * 同一明文两次编码结果不同，验证必须用 matches 而非字符串比对。
     */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

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

        // 密码验证（P046）：BCrypt 优先；存量 MD5 密码兼容回退，
        // MD5 验证通过后立即透明升级为 BCrypt 存库，全库密码自动完成迁移
        if (!verifyAndUpgradePassword(user, loginDTO.getPassword())) {
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

    /**
     * 验证密码：BCrypt matches 优先；未命中时按存量 MD5 比对，
     * MD5 命中说明该账号尚未迁移，随即重编码为 BCrypt 写回数据库。
     *
     * @return 密码是否验证通过
     */
    private boolean verifyAndUpgradePassword(User user, String rawPassword) {
        String stored = user.getPassword();
        if (PASSWORD_ENCODER.matches(rawPassword, stored)) {
            return true;
        }

        String passwordMd5 = DigestUtils.md5DigestAsHex(rawPassword.getBytes());
        if (!passwordMd5.equals(stored)) {
            return false;
        }

        // 存量 MD5 验证通过，透明升级为 BCrypt（只更新密码字段，不动其他列）
        User upgrade = new User();
        upgrade.setId(user.getId());
        upgrade.setPassword(PASSWORD_ENCODER.encode(rawPassword));
        userMapper.updateById(upgrade);
        return true;
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
