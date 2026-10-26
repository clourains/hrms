package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.PageResult;
import com.hrms.dto.RecruitQueryDTO;
import com.hrms.entity.Department;
import com.hrms.entity.Employee;
import com.hrms.entity.Probation;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.ProbationMapper;
import com.hrms.service.ProbationService;
import com.hrms.util.SortUtil;
import com.hrms.util.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 试用期转正Service实现类
 */
@Service
public class ProbationServiceImpl implements ProbationService {

    private static final Map<String, SortUtil.OrderCol> PROBATION_SORT = new HashMap<>();

    static {
        PROBATION_SORT.put("probationStart", SortUtil.OrderCol.plain("probation_start"));
        PROBATION_SORT.put("probationEnd", SortUtil.OrderCol.plain("probation_end"));
        PROBATION_SORT.put("status", SortUtil.OrderCol.plain("status"));
        PROBATION_SORT.put("score", SortUtil.OrderCol.plain("score"));
        PROBATION_SORT.put("createTime", SortUtil.OrderCol.plain("create_time"));
    }

    @Autowired
    private ProbationMapper probationMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Override
    public PageResult<Probation> pageQuery(RecruitQueryDTO queryDTO) {
        Page<Probation> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Probation> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String keyword = queryDTO.getKeyword();
            final List<Long> empIds = employeeMapper.selectList(new LambdaQueryWrapper<Employee>()
                            .select(Employee::getId)
                            .and(w -> w.like(Employee::getName, keyword).or().like(Employee::getEmpNo, keyword)))
                    .stream().map(Employee::getId).collect(Collectors.toList());
            if (empIds.isEmpty()) {
                // 姓名/工号都没命中时直接返回空页，避免退化成"查全部"
                return new PageResult<>(0L, java.util.Collections.emptyList());
            }
            wrapper.in(Probation::getEmployeeId, empIds);
        }
        if (queryDTO.getDeptId() != null) {
            wrapper.eq(Probation::getDeptId, queryDTO.getDeptId());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Probation::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getExpireDays() != null) {
            // 只看试用中且结束日期在 N 天内（含已逾期）的记录
            wrapper.eq(Probation::getStatus, Probation.STATUS_PROBATION)
                    .le(Probation::getProbationEnd, LocalDate.now().plusDays(queryDTO.getExpireDays()));
        }

        String orderBy = SortUtil.build(queryDTO.getSortField(), queryDTO.getSortOrder(), PROBATION_SORT);
        if (orderBy != null) {
            wrapper.last(orderBy);
        } else {
            // 默认：试用中的排前面，且快到期的最先出现
            wrapper.orderByAsc(Probation::getStatus)
                    .orderByAsc(Probation::getProbationEnd);
        }

        Page<Probation> result = probationMapper.selectPage(page, wrapper);
        fillExtras(result.getRecords());
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    /** 补员工姓名/工号/部门名和剩余天数 */
    private void fillExtras(List<Probation> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> empIds = records.stream().map(Probation::getEmployeeId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Employee> empMap = empIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : employeeMapper.selectBatchIds(empIds).stream()
                        .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));

        List<Long> deptIds = records.stream().map(Probation::getDeptId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> deptMap = deptIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : departmentMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));

        LocalDate today = LocalDate.now();
        for (Probation p : records) {
            Employee employee = empMap.get(p.getEmployeeId());
            if (employee != null) {
                p.setEmployeeName(employee.getName());
                p.setEmpNo(employee.getEmpNo());
            }
            if (p.getDeptId() != null) {
                p.setDeptName(deptMap.get(p.getDeptId()));
            }
            if (p.getProbationEnd() != null) {
                p.setDaysLeft(ChronoUnit.DAYS.between(today, p.getProbationEnd()));
            }
        }
    }

    @Override
    public Probation getById(Long id) {
        Probation probation = probationMapper.selectById(id);
        if (probation == null) {
            throw new RuntimeException("试用期记录不存在或已删除");
        }
        return probation;
    }

    @Override
    public boolean save(Probation probation) {
        validate(probation);
        ensureNoRunningRecord(probation.getEmployeeId(), null);
        if (probation.getStatus() == null) {
            probation.setStatus(Probation.STATUS_PROBATION);
        }
        probation.setId(null);
        probation.setConfirmDate(null);
        probation.setCreateTime(LocalDateTime.now());
        probation.setUpdateTime(LocalDateTime.now());
        return probationMapper.insert(probation) > 0;
    }

    @Override
    public boolean update(Probation probation) {
        if (probation.getId() == null) {
            throw new RuntimeException("试用期记录不存在");
        }
        Probation origin = probationMapper.selectById(probation.getId());
        if (origin == null) {
            throw new RuntimeException("试用期记录不存在或已删除");
        }
        validate(probation);
        ensureNoRunningRecord(probation.getEmployeeId(), probation.getId());
        // 状态与转正结论只走 confirm/fail/extend，表单不允许直接改
        probation.setStatus(origin.getStatus());
        probation.setConfirmDate(origin.getConfirmDate());
        probation.setUpdateTime(LocalDateTime.now());
        return probationMapper.updateById(probation) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        Probation origin = probationMapper.selectById(id);
        if (origin == null) {
            throw new RuntimeException("试用期记录不存在或已删除");
        }
        if (origin.getStatus() != null && origin.getStatus() == Probation.STATUS_PROBATION) {
            throw new RuntimeException("试用期还没结束，请先办理转正或终止，不能直接删除");
        }
        return probationMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirm(Long id, Integer score, String evaluation) {
        Probation probation = probationMapper.selectById(id);
        if (probation == null) {
            throw new RuntimeException("试用期记录不存在或已删除");
        }
        if (probation.getStatus() != null && probation.getStatus() != Probation.STATUS_PROBATION) {
            throw new RuntimeException("该员工已办理过转正结论，不能重复操作");
        }
        Validators.notNull(score, "转正评分");
        if (score < 0 || score > 100) {
            throw new RuntimeException("转正评分应在 0~100 之间");
        }

        Probation update = new Probation();
        update.setId(id);
        update.setStatus(Probation.STATUS_REGULAR);
        update.setConfirmDate(LocalDate.now());
        update.setScore(score);
        update.setEvaluation(evaluation);
        update.setUpdateTime(LocalDateTime.now());
        boolean ok = probationMapper.updateById(update) > 0;

        // 员工状态：2试用期 -> 1在职；同时把工龄基数从入职日补上
        if (ok && probation.getEmployeeId() != null) {
            Employee employee = new Employee();
            employee.setId(probation.getEmployeeId());
            employee.setStatus(1);
            employee.setUpdateTime(LocalDateTime.now());
            syncWorkYears(employee, probation.getProbationStart());
            employeeMapper.updateById(employee);
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean fail(Long id, String evaluation) {
        Probation probation = probationMapper.selectById(id);
        if (probation == null) {
            throw new RuntimeException("试用期记录不存在或已删除");
        }
        if (probation.getStatus() != null && probation.getStatus() != Probation.STATUS_PROBATION) {
            throw new RuntimeException("该员工已办理过转正结论，不能重复操作");
        }
        Validators.text(evaluation, "未通过原因");

        Probation update = new Probation();
        update.setId(id);
        update.setStatus(Probation.STATUS_FAILED);
        update.setConfirmDate(LocalDate.now());
        update.setEvaluation(evaluation);
        update.setUpdateTime(LocalDateTime.now());
        boolean ok = probationMapper.updateById(update) > 0;

        // 员工按离职处理，离职日期记今天，保证入离职趋势统计有数据
        if (ok && probation.getEmployeeId() != null) {
            Employee employee = new Employee();
            employee.setId(probation.getEmployeeId());
            employee.setStatus(0);
            employee.setLeaveDate(LocalDate.now());
            employee.setUpdateTime(LocalDateTime.now());
            employeeMapper.updateById(employee);
        }
        return ok;
    }

    @Override
    public boolean extend(Long id, LocalDate newEnd, String evaluation) {
        Probation probation = probationMapper.selectById(id);
        if (probation == null) {
            throw new RuntimeException("试用期记录不存在或已删除");
        }
        if (probation.getStatus() != null && probation.getStatus() != Probation.STATUS_PROBATION) {
            throw new RuntimeException("该员工已办理过转正结论，不能延长试用期");
        }
        Validators.notNull(newEnd, "新的试用期结束日期");
        if (probation.getProbationEnd() != null && !newEnd.isAfter(probation.getProbationEnd())) {
            throw new RuntimeException("新的结束日期必须晚于原结束日期");
        }
        if (newEnd.isAfter(LocalDate.now().plusMonths(7))) {
            throw new RuntimeException("试用期总长不宜超过 6 个月，请核对日期");
        }

        Probation update = new Probation();
        update.setId(id);
        update.setProbationEnd(newEnd);
        // 延期后仍是「试用中」，否则会从到期提醒里消失
        update.setStatus(Probation.STATUS_PROBATION);
        update.setEvaluation(StringUtils.hasText(evaluation) ? ("[延期] " + evaluation) : "[延期] 试用期延长至 " + newEnd);
        update.setUpdateTime(LocalDateTime.now());
        return probationMapper.updateById(update) > 0;
    }

    @Override
    public Map<String, Object> statistics() {
        LocalDate today = LocalDate.now();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("probationing", probationMapper.selectCount(new LambdaQueryWrapper<Probation>()
                .eq(Probation::getStatus, Probation.STATUS_PROBATION)));
        stats.put("expiringIn30", probationMapper.selectCount(new LambdaQueryWrapper<Probation>()
                .eq(Probation::getStatus, Probation.STATUS_PROBATION)
                .ge(Probation::getProbationEnd, today)
                .le(Probation::getProbationEnd, today.plusDays(30))));
        stats.put("overdue", probationMapper.selectCount(new LambdaQueryWrapper<Probation>()
                .eq(Probation::getStatus, Probation.STATUS_PROBATION)
                .lt(Probation::getProbationEnd, today)));
        stats.put("regularThisMonth", probationMapper.selectCount(new LambdaQueryWrapper<Probation>()
                .eq(Probation::getStatus, Probation.STATUS_REGULAR)
                .ge(Probation::getConfirmDate, today.withDayOfMonth(1))));
        stats.put("failedTotal", probationMapper.selectCount(new LambdaQueryWrapper<Probation>()
                .eq(Probation::getStatus, Probation.STATUS_FAILED)));
        return stats;
    }

    /** 同一员工同时只能有一条进行中的试用记录 */
    private void ensureNoRunningRecord(Long employeeId, Long excludeId) {
        LambdaQueryWrapper<Probation> wrapper = new LambdaQueryWrapper<Probation>()
                .eq(Probation::getEmployeeId, employeeId)
                .eq(Probation::getStatus, Probation.STATUS_PROBATION);
        if (excludeId != null) {
            wrapper.ne(Probation::getId, excludeId);
        }
        long count = probationMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("该员工已有一条进行中的试用期记录，请勿重复登记");
        }
    }

    /** 工龄按入职日期重算（转正当天顺手更新，避免员工列表里工龄一直是 0） */
    private void syncWorkYears(Employee employee, LocalDate probationStart) {
        if (probationStart == null) {
            return;
        }
        long years = ChronoUnit.YEARS.between(probationStart, LocalDate.now());
        employee.setWorkYears((int) Math.max(0, years));
    }

    /** 新增/编辑同一套必填口径：员工、部门、职位、试用期起止 */
    private void validate(Probation p) {
        Validators.notNull(p.getEmployeeId(), "员工");
        Validators.notNull(p.getDeptId(), "所属部门");
        Validators.text(p.getPosition(), "职位");
        Validators.notNull(p.getProbationStart(), "试用期开始日期");
        Validators.notNull(p.getProbationEnd(), "试用期结束日期");
        if (p.getProbationEnd().isBefore(p.getProbationStart())) {
            throw new RuntimeException("试用期结束日期不能早于开始日期");
        }
        Validators.nonNegative(p.getScore(), "转正评分");
        if (p.getScore() != null && p.getScore() > 100) {
            throw new RuntimeException("转正评分应在 0~100 之间");
        }
    }
}
