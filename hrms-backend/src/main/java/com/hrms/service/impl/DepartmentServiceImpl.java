package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.entity.Department;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.service.DepartmentService;
import com.hrms.util.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门Service实现类
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentMapper departmentMapper;

    @Override
    @Cacheable(cacheNames = "deptTree", key = "'tree'")
    public List<Department> tree() {
        // 获取所有部门
        List<Department> allDepts = listAll();
        // 构建树形结构
        return buildTree(allDepts, 0L);
    }

    /**
     * 递归构建部门树
     */
    private List<Department> buildTree(List<Department> allDepts, Long parentId) {
        List<Department> tree = new ArrayList<>();
        for (Department dept : allDepts) {
            if (parentId.equals(dept.getParentId())) {
                // 这里使用一个简单的方式，通过扩展字段来存储子节点
                // 由于实体类没有children字段，我们返回扁平列表，前端自行构建树
                // 或者我们可以创建一个VO类。这里为了简单，返回带层级的列表
                tree.add(dept);
                // 递归获取子部门
                List<Department> children = buildTree(allDepts, dept.getId());
                tree.addAll(children);
            }
        }
        return tree;
    }

    @Override
    public List<Department> listAll() {
        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Department::getSort);
        return departmentMapper.selectList(queryWrapper);
    }

    @Override
    public Department getById(Long id) {
        return departmentMapper.selectById(id);
    }

    @Override
    @CacheEvict(cacheNames = "deptTree", allEntries = true)
    public boolean save(Department department) {
        Validators.text(department.getDeptName(), "部门名称");
        Validators.text(department.getDeptCode(), "部门编码");
        if (department.getParentId() == null) {
            department.setParentId(0L);
        }
        department.setCreateTime(LocalDateTime.now());
        department.setUpdateTime(LocalDateTime.now());
        return departmentMapper.insert(department) > 0;
    }

    @Override
    @CacheEvict(cacheNames = "deptTree", allEntries = true)
    public boolean update(Department department) {
        if (department.getId() == null) {
            throw new RuntimeException("部门不存在");
        }
        Department origin = departmentMapper.selectById(department.getId());
        if (origin == null) {
            throw new RuntimeException("部门不存在或已删除");
        }
        Validators.notCleared(origin.getDeptName(), department.getDeptName(), "部门名称");
        Validators.notCleared(origin.getDeptCode(), department.getDeptCode(), "部门编码");
        department.setUpdateTime(LocalDateTime.now());
        return departmentMapper.updateById(department) > 0;
    }

    @Override
    @CacheEvict(cacheNames = "deptTree", allEntries = true)
    public boolean deleteById(Long id) {
        // 检查是否有子部门
        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Department::getParentId, id);
        Long count = departmentMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new RuntimeException("存在子部门，无法删除");
        }
        return departmentMapper.deleteById(id) > 0;
    }
}
