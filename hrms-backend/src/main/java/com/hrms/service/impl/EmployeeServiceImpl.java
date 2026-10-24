package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.EmployeeQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Employee;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.service.EmployeeService;
import com.hrms.util.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工Service实现类
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Override
    public PageResult<Employee> pageQuery(EmployeeQueryDTO queryDTO) {
        Page<Employee> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        // 姓名模糊查询
        if (StringUtils.hasText(queryDTO.getName())) {
            queryWrapper.like(Employee::getName, queryDTO.getName());
        }
        // 部门ID查询
        if (queryDTO.getDeptId() != null) {
            queryWrapper.eq(Employee::getDeptId, queryDTO.getDeptId());
        }
        // 职位模糊查询
        if (StringUtils.hasText(queryDTO.getPosition())) {
            queryWrapper.like(Employee::getPosition, queryDTO.getPosition());
        }
        // 状态查询
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(Employee::getStatus, queryDTO.getStatus());
        }
        // 按创建时间倒序
        queryWrapper.orderByDesc(Employee::getCreateTime);

        Page<Employee> resultPage = employeeMapper.selectPage(page, queryWrapper);

        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }

    @Override
    public Employee getById(Long id) {
        return employeeMapper.selectById(id);
    }

    @Override
    public boolean save(Employee employee) {
        Validators.text(employee.getEmpNo(), "工号");
        Validators.text(employee.getName(), "姓名");
        Validators.notNull(employee.getGender(), "性别");
        Validators.notNull(employee.getDeptId(), "部门");
        Validators.text(employee.getPosition(), "职位");
        Validators.text(employee.getPhone(), "手机号");
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        return employeeMapper.insert(employee) > 0;
    }

    @Override
    public boolean update(Employee employee) {
        if (employee.getId() == null) {
            throw new RuntimeException("员工不存在");
        }
        Employee origin = employeeMapper.selectById(employee.getId());
        if (origin == null) {
            throw new RuntimeException("员工不存在或已删除");
        }
        Validators.notCleared(origin.getEmpNo(), employee.getEmpNo(), "工号");
        Validators.notCleared(origin.getName(), employee.getName(), "姓名");
        Validators.notCleared(origin.getDeptId(), employee.getDeptId(), "部门");
        employee.setUpdateTime(LocalDateTime.now());
        return employeeMapper.updateById(employee) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return employeeMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteBatch(List<Long> ids) {
        return employeeMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public Long countTotal() {
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        return employeeMapper.selectCount(queryWrapper);
    }

    @Override
    public List<Employee> listAll() {
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Employee::getEmpNo);
        return employeeMapper.selectList(queryWrapper);
    }
}
