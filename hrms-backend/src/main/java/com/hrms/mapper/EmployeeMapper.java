package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 员工Mapper
 */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    /**
     * 取含已逻辑删除记录在内的最大工号数字后缀
     *
     * 录用候选人时要自动生成 EMPxxx 工号，而 sys_employee.emp_no 上有唯一索引 uk_emp_no，
     * 逻辑删除的员工行仍然占着工号。若按可见数据取最大值+1，删过员工之后就会撞唯一索引，
     * 报"该工号已存在"却查不到是谁占用。这里绕过逻辑删除直接取库里的真实最大值。
     */
    @Select("SELECT IFNULL(MAX(CAST(SUBSTRING(emp_no, 4) AS UNSIGNED)), 0) FROM sys_employee WHERE emp_no REGEXP '^EMP[0-9]+$'")
    Integer maxEmpNoSeqIncludeDeleted();
}
