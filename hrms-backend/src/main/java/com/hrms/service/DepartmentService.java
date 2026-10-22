package com.hrms.service;

import com.hrms.entity.Department;

import java.util.List;

/**
 * 部门Service接口
 */
public interface DepartmentService {

    /**
     * 获取部门树形结构
     *
     * @return 部门树
     */
    List<Department> tree();

    /**
     * 获取所有部门列表
     *
     * @return 部门列表
     */
    List<Department> listAll();

    /**
     * 根据ID获取部门信息
     *
     * @param id 部门ID
     * @return 部门信息
     */
    Department getById(Long id);

    /**
     * 新增部门
     *
     * @param department 部门信息
     * @return 是否成功
     */
    boolean save(Department department);

    /**
     * 更新部门信息
     *
     * @param department 部门信息
     * @return 是否成功
     */
    boolean update(Department department);

    /**
     * 根据ID删除部门
     *
     * @param id 部门ID
     * @return 是否成功
     */
    boolean deleteById(Long id);
}
