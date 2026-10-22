package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.EmployeeQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Employee;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.service.EmployeeService;
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
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        return employeeMapper.insert(employee) > 0;
    }

    @Override
    public boolean update(Employee employee) {
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
