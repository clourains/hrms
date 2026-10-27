package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.EmployeeQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Attendance;
import com.hrms.entity.Contract;
import com.hrms.entity.Employee;
import com.hrms.entity.Performance;
import com.hrms.entity.Salary;
import com.hrms.entity.User;
import com.hrms.mapper.AttendanceMapper;
import com.hrms.mapper.ContractMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.PerformanceMapper;
import com.hrms.mapper.SalaryMapper;
import com.hrms.mapper.UserMapper;
import com.hrms.service.EmployeeService;
import com.hrms.util.UserContext;
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

    @Autowired
    private EmployeeAccountSync accountSync;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private SalaryMapper salaryMapper;

    @Autowired
    private PerformanceMapper performanceMapper;

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageResult<Employee> pageQuery(EmployeeQueryDTO queryDTO) {
        Page<Employee> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        // 姓名模糊查询
        if (StringUtils.hasText(queryDTO.getName())) {
            queryWrapper.like(Employee::getName, queryDTO.getName());
        }
        // 工号模糊查询
        if (StringUtils.hasText(queryDTO.getEmpNo())) {
            queryWrapper.like(Employee::getEmpNo, queryDTO.getEmpNo());
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
        // P4-3 部门数据权限：部门经理强制只查本部门
        applyDeptScope(queryWrapper);
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
        boolean ok = employeeMapper.updateById(employee) > 0;

        // 在职状态变化要同步登录账号：离职停用、复职启用。
        // 原先不同步，离职员工的账号还能正常登录并看到原部门数据。
        if (ok && employee.getStatus() != null && !employee.getStatus().equals(origin.getStatus())) {
            if (employee.getStatus() == 0) {
                accountSync.deactivateByEmpId(employee.getId());
            } else if (employee.getStatus() == 1) {
                accountSync.activateByEmpId(employee.getId());
            }
        }
        return ok;
    }

    @Override
    public boolean deleteById(Long id) {
        assertNoRelatedRecords(id);
        boolean ok = employeeMapper.deleteById(id) > 0;
        if (ok) {
            accountSync.deactivateByEmpId(id);
        }
        return ok;
    }

    @Override
    public boolean deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (Long id : ids) {
            assertNoRelatedRecords(id);
        }
        boolean ok = employeeMapper.deleteBatchIds(ids) > 0;
        if (ok) {
            ids.forEach(accountSync::deactivateByEmpId);
        }
        return ok;
    }

    /**
     * 删除员工前检查关联数据。
     *
     * 原先删除员工只做 sys_employee 的逻辑删除，考勤/薪资/绩效/合同全部留在库里，
     * 列表页会显示成「未知员工」，薪资一键计算还会继续给已删员工算钱。
     * 有历史记录的员工不允许直接删，引导改用「状态=离职」（会同步停用账号）。
     */
    private void assertNoRelatedRecords(Long empId) {
        long att = attendanceMapper.selectCount(
                new LambdaQueryWrapper<Attendance>().eq(Attendance::getEmpId, empId));
        long sal = salaryMapper.selectCount(
                new LambdaQueryWrapper<Salary>().eq(Salary::getEmpId, empId));
        long perf = performanceMapper.selectCount(
                new LambdaQueryWrapper<Performance>().eq(Performance::getEmpId, empId));
        long contract = contractMapper.selectCount(
                new LambdaQueryWrapper<Contract>().eq(Contract::getEmpId, empId));
        long total = att + sal + perf + contract;
        if (total > 0) {
            throw new RuntimeException("该员工存在关联记录（考勤 " + att + " / 薪资 " + sal
                    + " / 绩效 " + perf + " / 合同 " + contract
                    + " 条），删除会留下无法显示的孤儿数据。请改为把状态置为「离职」，离职会同步停用其登录账号。");
        }
    }

    @Override
    public Long countTotal() {
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        return employeeMapper.selectCount(queryWrapper);
    }

    @Override
    public List<Employee> listAll() {
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        // P4-3 部门数据权限：部门经理仅返回本部门员工
        applyDeptScope(queryWrapper);
        queryWrapper.orderByAsc(Employee::getEmpNo);
        return employeeMapper.selectList(queryWrapper);
    }

    /**
     * 数据范围：admin 全部 / manager 本部门 / 普通员工仅本人。
     *
     * 原先对普通员工不追加任何条件，role=user 的账号调 /employee/page 或
     * /employee/list 能拿到全公司花名册（含手机号、邮箱、身份证），属水平越权。
     * 这里补上「仅本人」这一档；账号没有绑定员工档案时保持不加限制，
     * 避免把依赖员工列表找自己档案的自助页面（如考勤打卡）搞成空列表。
     */
    private void applyDeptScope(LambdaQueryWrapper<Employee> queryWrapper) {
        if (UserContext.isAdmin()) {
            return;
        }
        if (UserContext.isManager() && UserContext.getDeptId() != null) {
            queryWrapper.eq(Employee::getDeptId, UserContext.getDeptId());
            return;
        }
        Long myEmpId = currentEmpId();
        if (myEmpId != null) {
            queryWrapper.eq(Employee::getId, myEmpId);
        }
    }

    /** 按当前登录账号反查绑定的员工ID；未绑定返回 null */
    private Long currentEmpId() {
        String me = UserContext.get();
        if (me == null) {
            return null;
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, me));
        return user == null ? null : user.getEmpId();
    }
}
