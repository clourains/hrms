package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.EmployeeExitQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Department;
import com.hrms.entity.Employee;
import com.hrms.entity.EmployeeExit;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeExitMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.service.EmployeeExitService;
import com.hrms.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 入离管理（离职 / 辞退）Service 实现
 *
 * 流程：发起(0待审批) → 批准(1) / 驳回(2) → 生效(3) / 撤销(4)。
 * 「生效」是唯一会改员工档案的动作：status=0、leave_date、停用登录账号，
 * 审批过程中只动单据，避免 HR 还没交接完员工就已经被踢下线。
 */
@Service
public class EmployeeExitServiceImpl implements EmployeeExitService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private EmployeeExitMapper employeeExitMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private EmployeeAccountSync employeeAccountSync;

    @Override
    public PageResult<EmployeeExit> page(EmployeeExitQueryDTO query) {
        LambdaQueryWrapper<EmployeeExit> wrapper = new LambdaQueryWrapper<>();
        if (query.getExitKind() != null) {
            wrapper.eq(EmployeeExit::getExitKind, query.getExitKind());
        }
        if (query.getExitType() != null) {
            wrapper.eq(EmployeeExit::getExitType, query.getExitType());
        }
        if (query.getDeptId() != null) {
            wrapper.eq(EmployeeExit::getDeptId, query.getDeptId());
        }
        if (Boolean.TRUE.equals(query.getPendingOnly())) {
            wrapper.eq(EmployeeExit::getStatus, EmployeeExit.STATUS_PENDING);
        } else if (query.getStatus() != null) {
            wrapper.eq(EmployeeExit::getStatus, query.getStatus());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(EmployeeExit::getExpectLeaveDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(EmployeeExit::getExpectLeaveDate, query.getEndDate());
        }
        String keyword = query.getKeyword();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(EmployeeExit::getEmployeeName, keyword.trim())
                    .or().like(EmployeeExit::getEmpNo, keyword.trim())
                    .or().like(EmployeeExit::getExitNo, keyword.trim()));
        }
        wrapper.orderByDesc(EmployeeExit::getId);

        Page<EmployeeExit> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<EmployeeExit> result = employeeExitMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    public Map<String, Object> statistics(Integer kind) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("pending", count(kind, w -> w.eq(EmployeeExit::getStatus, EmployeeExit.STATUS_PENDING)));
        stat.put("approved", count(kind, w -> w.eq(EmployeeExit::getStatus, EmployeeExit.STATUS_APPROVED)));
        stat.put("effective", count(kind, w -> w.eq(EmployeeExit::getStatus, EmployeeExit.STATUS_EFFECTIVE)));
        stat.put("resignedThisMonth", count(EmployeeExit.KIND_RESIGN,
                w -> w.eq(EmployeeExit::getStatus, EmployeeExit.STATUS_EFFECTIVE)
                        .between(EmployeeExit::getActualLeaveDate, monthStart, monthEnd)));
        stat.put("dismissedThisMonth", count(EmployeeExit.KIND_DISMISS,
                w -> w.eq(EmployeeExit::getStatus, EmployeeExit.STATUS_EFFECTIVE)
                        .between(EmployeeExit::getActualLeaveDate, monthStart, monthEnd)));
        stat.put("compensationThisMonth", sumCompensation(monthStart, monthEnd));
        return stat;
    }

    private long count(Integer kind, java.util.function.Consumer<LambdaQueryWrapper<EmployeeExit>> extra) {
        LambdaQueryWrapper<EmployeeExit> wrapper = new LambdaQueryWrapper<>();
        if (kind != null) {
            wrapper.eq(EmployeeExit::getExitKind, kind);
        }
        extra.accept(wrapper);
        return employeeExitMapper.selectCount(wrapper);
    }

    private java.math.BigDecimal sumCompensation(LocalDate start, LocalDate end) {
        List<EmployeeExit> list = employeeExitMapper.selectList(new LambdaQueryWrapper<EmployeeExit>()
                .eq(EmployeeExit::getExitKind, EmployeeExit.KIND_DISMISS)
                .eq(EmployeeExit::getStatus, EmployeeExit.STATUS_EFFECTIVE)
                .between(EmployeeExit::getActualLeaveDate, start, end));
        return list.stream()
                .map(e -> e.getCompensation() == null ? java.math.BigDecimal.ZERO : e.getCompensation())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeExit create(EmployeeExit exit) {
        if (exit.getExitKind() == null
                || (exit.getExitKind() != EmployeeExit.KIND_RESIGN
                && exit.getExitKind() != EmployeeExit.KIND_DISMISS)) {
            throw new RuntimeException("请选择单据类别（离职或辞退）");
        }
        if (exit.getExitType() == null) {
            throw new RuntimeException("请选择离职/辞退类型");
        }
        if (exit.getExpectLeaveDate() == null) {
            throw new RuntimeException("请选择预计生效日期");
        }
        if (exit.getEmployeeId() == null) {
            throw new RuntimeException("请选择员工");
        }
        Employee employee = employeeMapper.selectById(exit.getEmployeeId());
        if (employee == null || Integer.valueOf(1).equals(employee.getDeleted())) {
            throw new RuntimeException("员工不存在或已删除");
        }
        if (employee.getStatus() != null && employee.getStatus() == 0) {
            throw new RuntimeException("该员工已是离职状态，无需再发起单据");
        }
        long active = employeeExitMapper.selectCount(new LambdaQueryWrapper<EmployeeExit>()
                .eq(EmployeeExit::getEmployeeId, employee.getId())
                .in(EmployeeExit::getStatus, EmployeeExit.STATUS_PENDING, EmployeeExit.STATUS_APPROVED));
        if (active > 0) {
            throw new RuntimeException("该员工已有未完成的离职/辞退单据，请先处理");
        }

        // 员工信息以档案为准，单据里只做快照，避免前端传错人
        exit.setEmployeeName(employee.getName());
        exit.setEmpNo(employee.getEmpNo());
        exit.setDeptId(employee.getDeptId());
        exit.setPosition(employee.getPosition());
        exit.setDeptName(deptName(employee.getDeptId()));
        exit.setExitNo(nextNo(exit.getExitKind()));
        exit.setStatus(EmployeeExit.STATUS_PENDING);
        exit.setApplyDate(LocalDate.now());
        exit.setActualLeaveDate(null);
        exit.setCompensation(exit.getCompensation() == null ? java.math.BigDecimal.ZERO : exit.getCompensation());
        exit.setCreateBy(UserContext.getOrDefault("unknown"));
        employeeExitMapper.insert(exit);
        return exit;
    }

    @Override
    public void update(EmployeeExit exit) {
        EmployeeExit db = require(idOf(exit));
        if (db.getStatus() != EmployeeExit.STATUS_PENDING) {
            throw new RuntimeException("只有待审批的单据可以修改");
        }
        db.setExitType(exit.getExitType());
        db.setExpectLeaveDate(exit.getExpectLeaveDate());
        db.setReason(exit.getReason());
        db.setHandoverTo(exit.getHandoverTo());
        db.setCompensation(exit.getCompensation() == null ? java.math.BigDecimal.ZERO : exit.getCompensation());
        employeeExitMapper.updateById(db);
    }

    @Override
    public void approve(Long id, String remark) {
        EmployeeExit db = require(id);
        if (db.getStatus() != EmployeeExit.STATUS_PENDING) {
            throw new RuntimeException("只有待审批的单据可以批准");
        }
        db.setStatus(EmployeeExit.STATUS_APPROVED);
        db.setApproveBy(UserContext.getOrDefault("unknown"));
        db.setApproveTime(LocalDateTime.now());
        db.setApproveRemark(remark);
        employeeExitMapper.updateById(db);
    }

    @Override
    public void reject(Long id, String remark) {
        EmployeeExit db = require(id);
        if (db.getStatus() != EmployeeExit.STATUS_PENDING) {
            throw new RuntimeException("只有待审批的单据可以驳回");
        }
        if (!StringUtils.hasText(remark)) {
            throw new RuntimeException("驳回必须填写原因");
        }
        db.setStatus(EmployeeExit.STATUS_REJECTED);
        db.setApproveBy(UserContext.getOrDefault("unknown"));
        db.setApproveTime(LocalDateTime.now());
        db.setApproveRemark(remark.trim());
        employeeExitMapper.updateById(db);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void effect(Long id) {
        EmployeeExit db = require(id);
        if (db.getStatus() != EmployeeExit.STATUS_APPROVED) {
            throw new RuntimeException("只有已批准的单据可以生效");
        }
        Employee employee = employeeMapper.selectById(db.getEmployeeId());
        if (employee == null || Integer.valueOf(1).equals(employee.getDeleted())) {
            throw new RuntimeException("员工不存在或已删除");
        }
        if (employee.getStatus() != null && employee.getStatus() == 0) {
            throw new RuntimeException("员工已是离职状态，无需重复生效");
        }

        LocalDate leaveDate = LocalDate.now();
        Employee update = new Employee();
        update.setId(employee.getId());
        update.setStatus(0);
        update.setLeaveDate(leaveDate);
        employeeMapper.updateById(update);
        // 同步停用登录账号，避免离职/被辞退员工仍能登录、打卡、看原部门数据
        employeeAccountSync.deactivateByEmpId(employee.getId());

        db.setStatus(EmployeeExit.STATUS_EFFECTIVE);
        db.setActualLeaveDate(leaveDate);
        if (!StringUtils.hasText(db.getApproveBy())) {
            db.setApproveBy(UserContext.getOrDefault("unknown"));
            db.setApproveTime(LocalDateTime.now());
        }
        employeeExitMapper.updateById(db);
    }

    @Override
    public void cancel(Long id) {
        EmployeeExit db = require(id);
        if (db.getStatus() != EmployeeExit.STATUS_PENDING && db.getStatus() != EmployeeExit.STATUS_APPROVED) {
            throw new RuntimeException("只有待审批或已批准的单据可以撤销");
        }
        db.setStatus(EmployeeExit.STATUS_CANCELED);
        employeeExitMapper.updateById(db);
    }

    @Override
    public void delete(Long id) {
        EmployeeExit db = require(id);
        if (db.getStatus() == EmployeeExit.STATUS_EFFECTIVE) {
            throw new RuntimeException("已生效的单据不能删除，请保留完整入离记录");
        }
        employeeExitMapper.deleteById(id);
    }

    private EmployeeExit require(Long id) {
        if (id == null) {
            throw new RuntimeException("单据ID不能为空");
        }
        EmployeeExit db = employeeExitMapper.selectById(id);
        if (db == null) {
            throw new RuntimeException("单据不存在或已删除");
        }
        return db;
    }

    private Long idOf(EmployeeExit exit) {
        return exit == null ? null : exit.getId();
    }

    private String deptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        Department dept = departmentMapper.selectById(deptId);
        return dept == null ? null : dept.getDeptName();
    }

    /** 单据号：离职 LZ、辞退 CT + 秒级时间戳 + 3 位随机数，保证并发下也不撞库 */
    private String nextNo(Integer kind) {
        String prefix = kind != null && kind == EmployeeExit.KIND_DISMISS ? "CT" : "LZ";
        for (int i = 0; i < 5; i++) {
            String no = prefix + LocalDateTime.now().format(NO_FMT)
                    + ThreadLocalRandom.current().nextInt(100, 999);
            long exists = employeeExitMapper.selectCount(new LambdaQueryWrapper<EmployeeExit>()
                    .eq(EmployeeExit::getExitNo, no));
            if (exists == 0) {
                return no;
            }
        }
        throw new RuntimeException("单据号生成失败，请稍后重试");
    }
}
