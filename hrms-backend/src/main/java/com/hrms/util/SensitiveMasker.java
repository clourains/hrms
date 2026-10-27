package com.hrms.util;

import com.hrms.entity.Employee;

import java.util.List;

/**
 * 员工敏感字段脱敏（PII 保护）
 *
 * 身份证号、薪资、护照号、家庭地址属于敏感信息：
 * 仅 admin 角色可见全量，manager / user 访问员工查询接口时一律置空，
 * 避免"任意登录账号即可拉取全公司薪资与身份证号"。
 *
 * 员工查看本人信息走 /self/** 接口，不受此处影响。
 */
public final class SensitiveMasker {

    private SensitiveMasker() {
    }

    /**
     * 当前登录人是否允许查看敏感字段
     */
    public static boolean canSeeSensitive() {
        return UserContext.isAdmin();
    }

    /**
     * 按当前登录人角色脱敏单个员工对象（就地修改）
     */
    public static Employee mask(Employee employee) {
        if (employee == null || canSeeSensitive()) {
            return employee;
        }
        employee.setIdCard(null);
        employee.setSalary(null);
        employee.setPassportNo(null);
        employee.setAddress(null);
        return employee;
    }

    /**
     * 批量脱敏
     */
    public static List<Employee> mask(List<Employee> employees) {
        if (employees == null || employees.isEmpty() || canSeeSensitive()) {
            return employees;
        }
        for (Employee employee : employees) {
            employee.setIdCard(null);
            employee.setSalary(null);
            employee.setPassportNo(null);
            employee.setAddress(null);
        }
        return employees;
    }
}
