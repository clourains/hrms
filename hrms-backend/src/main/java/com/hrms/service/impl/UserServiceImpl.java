package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.LoginDTO;
import com.hrms.entity.LoginLog;
import com.hrms.entity.User;
import com.hrms.mapper.LoginLogMapper;
import com.hrms.mapper.UserMapper;
import com.hrms.service.UserService;
import com.hrms.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户Service实现类
 */
@Service
public class UserServiceImpl implements UserService {

    /**
     * BCrypt 加盐编码器（P046，bean 由 SecurityConfig 提供）。默认强度 10，
     * 每次 encode 自动生成随机盐，同一明文两次编码结果不同，
     * 验证必须用 matches 而非字符串比对。
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Map<String, Object> login(LoginDTO loginDTO, String ip, String userAgent) {
        try {
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

            recordLoginLog(loginDTO.getUsername(), 1, "登录成功", ip, userAgent);
            return result;
        } catch (RuntimeException e) {
            recordLoginLog(loginDTO.getUsername(), 0, e.getMessage(), ip, userAgent);
            throw e;
        }
    }

    /**
     * P5-2 写登录日志；日志写失败不阻断登录主流程
     */
    private void recordLoginLog(String username, Integer success, String msg, String ip, String userAgent) {
        try {
            LoginLog log = new LoginLog();
            log.setUsername(username);
            log.setSuccess(success);
            log.setMsg(msg);
            log.setIp(ip);
            // UA 可能很长，截断防溢出
            log.setUserAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent);
            log.setCreateTime(LocalDateTime.now());
            loginLogMapper.insert(log);
        } catch (Exception ignored) {
        }
    }

    /**
     * 验证密码：BCrypt matches 优先；未命中时按存量 MD5 比对，
     * MD5 命中说明该账号尚未迁移，随即重编码为 BCrypt 写回数据库。
     *
     * @return 密码是否验证通过
     */
    private boolean verifyAndUpgradePassword(User user, String rawPassword) {
        String stored = user.getPassword();
        if (passwordEncoder.matches(rawPassword, stored)) {
            return true;
        }

        String passwordMd5 = DigestUtils.md5DigestAsHex(rawPassword.getBytes());
        if (!passwordMd5.equals(stored)) {
            return false;
        }

        // 存量 MD5 验证通过，透明升级为 BCrypt（只更新密码字段，不动其他列）
        User upgrade = new User();
        upgrade.setId(user.getId());
        upgrade.setPassword(passwordEncoder.encode(rawPassword));
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

    @Override
    public Page<User> pageQuery(long current, long size, String keyword, String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(User::getUsername, keyword)
                        .or().like(User::getNickname, keyword))
                .eq(StringUtils.hasText(role), User::getRole, role)
                .orderByAsc(User::getId);
        Page<User> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        page.getRecords().forEach(u -> u.setPassword(null));
        return page;
    }

    @Override
    public User createUser(User user) {
        if (!StringUtils.hasText(user.getUsername())) {
            throw new RuntimeException("用户名不能为空");
        }
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
        if (count > 0) {
            throw new RuntimeException("用户名已存在：" + user.getUsername());
        }
        // 默认密码 123456，BCrypt 编码入库
        String rawPassword = StringUtils.hasText(user.getPassword()) ? user.getPassword() : "123456";
        user.setPassword(passwordEncoder.encode(rawPassword));
        if (!StringUtils.hasText(user.getRole())) {
            user.setRole("user");
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        user.setId(null);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public void updateUser(User user, String operator) {
        User existing = userMapper.selectById(user.getId());
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }
        boolean self = existing.getUsername().equals(operator);
        // 自保：禁止禁用/降级当前登录的管理员账号，防止把自己锁在系统外
        if (self && user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("不能禁用当前登录的账号");
        }
        if (self && StringUtils.hasText(user.getRole()) && !"admin".equals(user.getRole())
                && "admin".equals(existing.getRole())) {
            throw new RuntimeException("不能降级当前登录的管理员账号");
        }

        User update = new User();
        update.setId(existing.getId());
        update.setNickname(user.getNickname());
        update.setRole(user.getRole());
        update.setEmpId(user.getEmpId());
        update.setStatus(user.getStatus());
        update.setAvatar(user.getAvatar());
        update.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(update);
    }

    @Override
    public void deleteUser(Long id, String operator) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }
        if (existing.getUsername().equals(operator)) {
            throw new RuntimeException("不能删除当前登录的账号");
        }
        userMapper.deleteById(id);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(oldPassword)) {
            throw new RuntimeException("请输入原密码");
        }
        if (!StringUtils.hasText(newPassword)) {
            throw new RuntimeException("请输入新密码");
        }
        if (newPassword.length() < 6 || newPassword.length() > 32) {
            throw new RuntimeException("新密码长度需为 6-32 位");
        }
        User existing = userMapper.selectById(userId);
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!verifyAndUpgradePassword(existing, oldPassword)) {
            throw new RuntimeException("原密码不正确");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        update.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(update);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }
        String raw = StringUtils.hasText(newPassword) ? newPassword : "123456";
        User update = new User();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(raw));
        update.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(update);
    }
}
