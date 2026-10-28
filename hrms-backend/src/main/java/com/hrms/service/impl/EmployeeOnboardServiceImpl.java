package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.EmployeeOnboardQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Department;
import com.hrms.entity.Employee;
import com.hrms.entity.EmployeeOnboard;
import com.hrms.entity.Probation;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.EmployeeOnboardMapper;
import com.hrms.mapper.ProbationMapper;
import com.hrms.service.EmployeeOnboardService;
import com.hrms.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 入职登记 Service 实现
 *
 * 登记不直接写员工档案，先落在登记台账；档案关联有两种方式：
 *   1) 关联已有档案：适合信息已在员工管理里的老员工/补录
 *   2) 一键新建档案：按登记信息生成员工档案 + 试用期记录，工号自动生成
 * 这样入职资料可以先收、后落档，也不会重复建档。
 */
@Service
public class EmployeeOnboardServiceImpl implements EmployeeOnboardService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private EmployeeOnboardMapper onboardMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private ProbationMapper probationMapper;

    @Override
    public PageResult<EmployeeOnboard> page(EmployeeOnboardQueryDTO query) {
        LambdaQueryWrapper<EmployeeOnboard> wrapper = new LambdaQueryWrapper<>();
        if (query.getDeptId() != null) {
            wrapper.eq(EmployeeOnboard::getDeptId, query.getDeptId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(EmployeeOnboard::getStatus, query.getStatus());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(EmployeeOnboard::getEntryDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(EmployeeOnboard::getEntryDate, query.getEndDate());
        }
        String keyword = query.getKeyword();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(EmployeeOnboard::getName, keyword.trim())
                    .or().like(EmployeeOnboard::getPosition, keyword.trim())
                    .or().like(EmployeeOnboard::getOnboardNo, keyword.trim()));
        }
        wrapper.orderByDesc(EmployeeOnboard::getEntryDate).orderByDesc(EmployeeOnboard::getId);

        Page<EmployeeOnboard> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<EmployeeOnboard> result = onboardMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    public Map<String, Object> statistics() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("total", onboardMapper.selectCount(null));
        stat.put("pending", onboardMapper.selectCount(new LambdaQueryWrapper<EmployeeOnboard>()
                .eq(EmployeeOnboard::getStatus, EmployeeOnboard.STATUS_UNLINKED)));
        stat.put("linked", onboardMapper.selectCount(new LambdaQueryWrapper<EmployeeOnboard>()
                .eq(EmployeeOnboard::getStatus, EmployeeOnboard.STATUS_LINKED)));
        stat.put("thisMonth", onboardMapper.selectCount(new LambdaQueryWrapper<EmployeeOnboard>()
                .between(EmployeeOnboard::getEntryDate, monthStart, monthEnd)));
        stat.put("upcoming", onboardMapper.selectCount(new LambdaQueryWrapper<EmployeeOnboard>()
                .gt(EmployeeOnboard::getEntryDate, today)));
        return stat;
    }

    @Override
    public EmployeeOnboard create(EmployeeOnboard onboard) {
        validate(onboard);
        onboard.setDeptName(deptName(onboard.getDeptId()));
        onboard.setProbationMonths(onboard.getProbationMonths() == null || onboard.getProbationMonths() < 1
                ? 3 : onboard.getProbationMonths());
        onboard.setStatus(EmployeeOnboard.STATUS_UNLINKED);
        onboard.setEmployeeId(null);
        onboard.setOnboardNo(nextNo());
        onboard.setCreateBy(UserContext.getOrDefault("unknown"));
        onboardMapper.insert(onboard);
        return onboard;
    }

    @Override
    public void update(EmployeeOnboard onboard) {
        EmployeeOnboard db = require(onboard == null ? null : onboard.getId());
        if (db.getStatus() == EmployeeOnboard.STATUS_LINKED) {
            throw new RuntimeException("已关联档案的登记不能再改基础信息，请到员工管理修改档案");
        }
        if (!StringUtils.hasText(onboard.getName())) {
            throw new RuntimeException("姓名不能为空");
        }
        if (onboard.getEntryDate() == null) {
            throw new RuntimeException("请选择入职日期");
        }
        if (onboard.getDeptId() == null) {
            throw new RuntimeException("请选择部门");
        }
        if (!StringUtils.hasText(onboard.getPosition())) {
            throw new RuntimeException("岗位不能为空");
        }
        db.setName(onboard.getName().trim());
        db.setGender(onboard.getGender());
        db.setPhone(onboard.getPhone());
        db.setDeptId(onboard.getDeptId());
        db.setDeptName(deptName(onboard.getDeptId()));
        db.setPosition(onboard.getPosition().trim());
        db.setEntryDate(onboard.getEntryDate());
        db.setProbationMonths(onboard.getProbationMonths() == null || onboard.getProbationMonths() < 1
                ? 3 : onboard.getProbationMonths());
        db.setSource(onboard.getSource());
        db.setRemark(onboard.getRemark());
        onboardMapper.updateById(db);
    }

    @Override
    public void link(Long id, Long employeeId) {
        if (employeeId == null) {
            throw new RuntimeException("请选择要关联的员工档案");
        }
        EmployeeOnboard db = require(id);
        if (db.getStatus() == EmployeeOnboard.STATUS_LINKED && db.getEmployeeId() != null) {
            throw new RuntimeException("该登记已关联档案，如需改关联请先解除或删除登记");
        }
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new RuntimeException("员工档案不存在或已删除");
        }
        if (employee.getStatus() != null && employee.getStatus() == 0) {
            throw new RuntimeException("该员工已是离职状态，不能关联入职登记");
        }
        long linked = onboardMapper.selectCount(new LambdaQueryWrapper<EmployeeOnboard>()
                .eq(EmployeeOnboard::getEmployeeId, employeeId)
                .eq(EmployeeOnboard::getStatus, EmployeeOnboard.STATUS_LINKED));
        if (linked > 0) {
            throw new RuntimeException("该员工档案已关联了其他入职登记，不能重复关联");
        }
        db.setEmployeeId(employeeId);
        db.setStatus(EmployeeOnboard.STATUS_LINKED);
        onboardMapper.updateById(db);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProfile(Long id) {
        EmployeeOnboard db = require(id);
        if (db.getStatus() == EmployeeOnboard.STATUS_LINKED && db.getEmployeeId() != null) {
            throw new RuntimeException("该登记已关联档案，无需重复建档");
        }
        if (db.getGender() == null) {
            throw new RuntimeException("建档前请先补全性别");
        }
        if (!StringUtils.hasText(db.getPhone())) {
            throw new RuntimeException("建档前请先补全手机号");
        }

        Employee employee = new Employee();
        employee.setEmpNo(nextEmpNo());
        employee.setName(db.getName());
        employee.setGender(db.getGender());
        employee.setPhone(db.getPhone());
        employee.setDeptId(db.getDeptId());
        employee.setPosition(db.getPosition());
        employee.setEntryDate(db.getEntryDate());
        // 2 = 试用期，与员工管理里的状态口径一致
        employee.setStatus(2);
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        employeeMapper.insert(employee);

        int months = db.getProbationMonths() == null || db.getProbationMonths() < 1 ? 3 : db.getProbationMonths();
        Probation probation = new Probation();
        probation.setEmployeeId(employee.getId());
        probation.setDeptId(db.getDeptId());
        probation.setPosition(db.getPosition());
        probation.setProbationStart(db.getEntryDate());
        probation.setProbationEnd(db.getEntryDate().plusMonths(months));
        // 1 = 试用中
        probation.setStatus(1);
        probation.setCreateTime(LocalDateTime.now());
        probation.setUpdateTime(LocalDateTime.now());
        probationMapper.insert(probation);

        db.setEmployeeId(employee.getId());
        db.setStatus(EmployeeOnboard.STATUS_LINKED);
        onboardMapper.updateById(db);
        return employee.getId();
    }

    @Override
    public void delete(Long id) {
        EmployeeOnboard db = require(id);
        // 只删登记台账，不动员工档案；已关联档案的删除前由前端二次确认
        onboardMapper.deleteById(db.getId());
    }

    private void validate(EmployeeOnboard onboard) {
        if (onboard == null) {
            throw new RuntimeException("登记信息不能为空");
        }
        if (!StringUtils.hasText(onboard.getName())) {
            throw new RuntimeException("姓名不能为空");
        }
        if (onboard.getEntryDate() == null) {
            throw new RuntimeException("请选择入职日期");
        }
        if (onboard.getDeptId() == null) {
            throw new RuntimeException("请选择部门");
        }
        if (!StringUtils.hasText(onboard.getPosition())) {
            throw new RuntimeException("岗位不能为空");
        }
    }

    private EmployeeOnboard require(Long id) {
        if (id == null) {
            throw new RuntimeException("登记ID不能为空");
        }
        EmployeeOnboard db = onboardMapper.selectById(id);
        if (db == null) {
            throw new RuntimeException("登记记录不存在或已删除");
        }
        return db;
    }

    private String deptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        Department dept = departmentMapper.selectById(deptId);
        return dept == null ? null : dept.getDeptName();
    }

    /** 登记编号：RD + 秒级时间戳 + 3 位随机数 */
    private String nextNo() {
        for (int i = 0; i < 5; i++) {
            String no = "RD" + LocalDateTime.now().format(NO_FMT) + ThreadLocalRandom.current().nextInt(100, 999);
            if (onboardMapper.selectCount(new LambdaQueryWrapper<EmployeeOnboard>()
                    .eq(EmployeeOnboard::getOnboardNo, no)) == 0) {
                return no;
            }
        }
        throw new RuntimeException("登记编号生成失败，请稍后重试");
    }

    /** 员工工号：OB + 年月日 + 3 位随机数，保证不与现有工号重复 */
    private String nextEmpNo() {
        String day = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        for (int i = 0; i < 10; i++) {
            String empNo = "OB" + day + ThreadLocalRandom.current().nextInt(100, 999);
            if (employeeMapper.selectCount(new LambdaQueryWrapper<Employee>()
                    .eq(Employee::getEmpNo, empNo)) == 0) {
                return empNo;
            }
        }
        throw new RuntimeException("工号生成失败，请稍后重试");
    }
}
