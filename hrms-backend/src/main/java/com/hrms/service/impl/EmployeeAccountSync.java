package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.entity.User;
import com.hrms.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 员工状态 ↔ 登录账号状态 的联动（P1 生命周期闭环）
 *
 * 原先员工离职（status=0）只是改了 sys_employee，sys_user 完全不动，
 * 离职员工照样能登录、看到原部门数据、发起审批和打卡。
 * 这里把「停用/启用账号」收口到一处，员工管理和试用期转正共用。
 */
@Component
public class EmployeeAccountSync {

    @Autowired
    private UserMapper userMapper;

    /** 停用该员工绑定的所有登录账号（正常情况只有一个） */
    public int deactivateByEmpId(Long empId) {
        return setStatus(empId, 0);
    }

    /** 重新启用（复职场景） */
    public int activateByEmpId(Long empId) {
        return setStatus(empId, 1);
    }

    private int setStatus(Long empId, int status) {
        if (empId == null) {
            return 0;
        }
        User update = new User();
        update.setStatus(status);
        update.setUpdateTime(LocalDateTime.now());
        return userMapper.update(update, new LambdaQueryWrapper<User>().eq(User::getEmpId, empId));
    }
}
