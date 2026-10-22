package com.hrms.service;

import com.hrms.dto.EmployeeQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Employee;

import java.util.List;

/**
 * 员工Service接口
 */
public interface EmployeeService {

    /**
     * 分页查询员工列表
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<Employee> pageQuery(EmployeeQueryDTO queryDTO);

    /**
     * 根据ID获取员工信息
     *
     * @param id 员工ID
     * @return 员工信息
     */
    Employee getById(Long id);

    /**
     * 新增员工
     *
     * @param employee 员工信息
     * @return 是否成功
     */
    boolean save(Employee employee);

    /**
     * 更新员工信息
     *
     * @param employee 员工信息
     * @return 是否成功
     */
    boolean update(Employee employee);

    /**
     * 根据ID删除员工
     *
     * @param id 员工ID
     * @return 是否成功
     */
    boolean deleteById(Long id);

    /**
     * 批量删除员工
     *
     * @param ids 员工ID列表
     * @return 是否成功
     */
    boolean deleteBatch(List<Long> ids);

    /**
     * 统计员工总数
     *
     * @return 员工总数
     */
    Long countTotal();

    /**
     * 获取所有员工列表
     *
     * @return 员工列表
     */
    List<Employee> listAll();
}
