package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.HireDTO;
import com.hrms.dto.PageResult;
import com.hrms.dto.RecruitQueryDTO;
import com.hrms.entity.Candidate;
import com.hrms.entity.CandidateInterview;
import com.hrms.entity.Department;
import com.hrms.entity.Employee;
import com.hrms.entity.JobRequisition;
import com.hrms.entity.Probation;
import com.hrms.entity.User;
import com.hrms.mapper.CandidateInterviewMapper;
import com.hrms.mapper.CandidateMapper;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.JobRequisitionMapper;
import com.hrms.mapper.ProbationMapper;
import com.hrms.mapper.UserMapper;
import com.hrms.service.CandidateService;
import com.hrms.util.CodeReleaser;
import com.hrms.util.SortUtil;
import com.hrms.util.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 招聘候选人Service实现类
 */
@Service
public class CandidateServiceImpl implements CandidateService {

    private static final DateTimeFormatter NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** hr_recruit_candidate.candidate_no varchar(32) */
    private static final int CANDIDATE_NO_MAX_LEN = 32;

    private static final Map<String, SortUtil.OrderCol> CANDIDATE_SORT = new HashMap<>();

    /** 阶段名称，统计漏斗和淘汰提示共用 */
    private static final String[] STAGE_NAMES = {"待筛选", "初试", "复试", "待录用", "已录用", "已淘汰"};

    /** 录用时自动开通的账号初始密码（与 UserServiceImpl 默认值一致） */
    private static final String DEFAULT_PASSWORD = "123456";

    static {
        CANDIDATE_SORT.put("candidateNo", SortUtil.OrderCol.natural("candidate_no"));
        CANDIDATE_SORT.put("name", SortUtil.OrderCol.natural("name"));
        CANDIDATE_SORT.put("workYears", SortUtil.OrderCol.plain("work_years"));
        CANDIDATE_SORT.put("expectSalary", SortUtil.OrderCol.plain("expect_salary"));
        CANDIDATE_SORT.put("stage", SortUtil.OrderCol.plain("stage"));
        CANDIDATE_SORT.put("createTime", SortUtil.OrderCol.plain("create_time"));
    }

    @Autowired
    private CandidateMapper candidateMapper;

    @Autowired
    private JobRequisitionMapper requisitionMapper;

    @Autowired
    private CandidateInterviewMapper interviewMapper;

    @Autowired
    private ProbationMapper probationMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public PageResult<Candidate> pageQuery(RecruitQueryDTO queryDTO) {
        Page<Candidate> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Candidate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Candidate::getName, queryDTO.getKeyword())
                    .or().like(Candidate::getPhone, queryDTO.getKeyword())
                    .or().like(Candidate::getCandidateNo, queryDTO.getKeyword())
                    .or().like(Candidate::getCurrentCompany, queryDTO.getKeyword()));
        }
        if (queryDTO.getRequisitionId() != null) {
            wrapper.eq(Candidate::getRequisitionId, queryDTO.getRequisitionId());
        }
        if (queryDTO.getStage() != null) {
            wrapper.eq(Candidate::getStage, queryDTO.getStage());
        }
        if (StringUtils.hasText(queryDTO.getSource())) {
            wrapper.eq(Candidate::getSource, queryDTO.getSource());
        }
        // 高级筛选：学历 / 年限区间 / 期望薪资区间 / 录入时间 / 是否已安排面试
        if (StringUtils.hasText(queryDTO.getEducation())) {
            wrapper.eq(Candidate::getEducation, queryDTO.getEducation());
        }
        if (queryDTO.getWorkYearsMin() != null) {
            wrapper.ge(Candidate::getWorkYears, queryDTO.getWorkYearsMin());
        }
        if (queryDTO.getWorkYearsMax() != null) {
            wrapper.le(Candidate::getWorkYears, queryDTO.getWorkYearsMax());
        }
        if (queryDTO.getSalaryMin() != null) {
            wrapper.ge(Candidate::getExpectSalary, queryDTO.getSalaryMin());
        }
        if (queryDTO.getSalaryMax() != null) {
            wrapper.le(Candidate::getExpectSalary, queryDTO.getSalaryMax());
        }
        if (queryDTO.getCreateTimeStart() != null) {
            wrapper.ge(Candidate::getCreateTime, queryDTO.getCreateTimeStart().atStartOfDay());
        }
        if (queryDTO.getCreateTimeEnd() != null) {
            wrapper.le(Candidate::getCreateTime, queryDTO.getCreateTimeEnd().atTime(23, 59, 59));
        }
        // 面试情况用子查询过滤：分页后再在内存筛会导致 total 与列表不一致
        if (queryDTO.getHasInterview() != null) {
            String interviewedSql = "SELECT DISTINCT candidate_id FROM hr_recruit_interview WHERE deleted = 0";
            if (queryDTO.getHasInterview() == 1) {
                wrapper.inSql(Candidate::getId, interviewedSql);
            } else {
                wrapper.notInSql(Candidate::getId, interviewedSql);
            }
        }

        String orderBy = SortUtil.build(queryDTO.getSortField(), queryDTO.getSortOrder(), CANDIDATE_SORT);
        if (orderBy != null) {
            wrapper.last(orderBy);
        } else {
            wrapper.orderByDesc(Candidate::getCreateTime);
        }

        Page<Candidate> result = candidateMapper.selectPage(page, wrapper);
        fillExtras(result.getRecords());
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    /** 批量补岗位/部门名称和已面试轮次 */
    private void fillExtras(List<Candidate> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> reqIds = records.stream().map(Candidate::getRequisitionId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, JobRequisition> reqMap = reqIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : requisitionMapper.selectBatchIds(reqIds).stream()
                        .collect(Collectors.toMap(JobRequisition::getId, r -> r, (a, b) -> a));

        List<Long> deptIds = reqMap.values().stream().map(JobRequisition::getDeptId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> deptMap = deptIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : departmentMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));

        for (Candidate c : records) {
            JobRequisition req = reqMap.get(c.getRequisitionId());
            if (req != null) {
                c.setRequisitionPosition(req.getPosition());
                if (req.getDeptId() != null) {
                    c.setDeptName(deptMap.get(req.getDeptId()));
                }
            }
            long count = interviewMapper.selectCount(new LambdaQueryWrapper<CandidateInterview>()
                    .eq(CandidateInterview::getCandidateId, c.getId()));
            c.setInterviewCount(Math.toIntExact(count));
        }
    }

    @Override
    public Candidate getById(Long id) {
        Candidate candidate = candidateMapper.selectById(id);
        if (candidate == null) {
            throw new RuntimeException("候选人不存在或已删除");
        }
        List<Candidate> one = new ArrayList<>();
        one.add(candidate);
        fillExtras(one);
        return candidate;
    }

    @Override
    public boolean save(Candidate candidate) {
        validate(candidate);
        JobRequisition req = requisitionMapper.selectById(candidate.getRequisitionId());
        if (req == null) {
            throw new RuntimeException("应聘的招聘需求不存在或已删除");
        }
        if (req.getStatus() != null && req.getStatus() == JobRequisition.STATUS_CLOSED) {
            throw new RuntimeException("该招聘需求已关闭，不能再登记候选人");
        }
        if (candidate.getGender() == null) {
            candidate.setGender(1);
        }
        if (candidate.getStage() == null) {
            candidate.setStage(Candidate.STAGE_SCREENING);
        }
        if (candidate.getWorkYears() == null) {
            candidate.setWorkYears(0);
        }
        candidate.setId(null);
        candidate.setEmployeeId(null);
        candidate.setCandidateNo(generateCandidateNo());
        candidate.setCreateTime(LocalDateTime.now());
        candidate.setUpdateTime(LocalDateTime.now());
        return candidateMapper.insert(candidate) > 0;
    }

    @Override
    public boolean update(Candidate candidate) {
        if (candidate.getId() == null) {
            throw new RuntimeException("候选人不存在");
        }
        Candidate origin = candidateMapper.selectById(candidate.getId());
        if (origin == null) {
            throw new RuntimeException("候选人不存在或已删除");
        }
        validate(candidate);
        // 编号固定；已录用/已淘汰的结果只能通过面试登记和录用动作改变，不允许表单直接改阶段
        candidate.setCandidateNo(origin.getCandidateNo());
        candidate.setEmployeeId(origin.getEmployeeId());
        candidate.setStage(origin.getStage());
        candidate.setUpdateTime(LocalDateTime.now());
        return candidateMapper.updateById(candidate) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        Candidate candidate = candidateMapper.selectById(id);
        if (candidate == null) {
            throw new RuntimeException("候选人不存在或已删除");
        }
        if (candidate.getEmployeeId() != null) {
            throw new RuntimeException("该候选人已录用并生成员工档案，不能删除");
        }
        // 面试记录先逻辑删掉，避免留下指向不存在候选人的孤儿记录
        List<CandidateInterview> interviews = interviewMapper.selectList(
                new LambdaQueryWrapper<CandidateInterview>().eq(CandidateInterview::getCandidateId, id));
        for (CandidateInterview interview : interviews) {
            interviewMapper.deleteById(interview.getId());
        }

        Candidate release = new Candidate();
        release.setId(id);
        release.setCandidateNo(CodeReleaser.release(candidate.getCandidateNo(), id, CANDIDATE_NO_MAX_LEN));
        release.setUpdateTime(LocalDateTime.now());
        candidateMapper.updateById(release);

        return candidateMapper.deleteById(id) > 0;
    }

    @Override
    public boolean changeStage(Long id, Integer stage, String reason) {
        if (stage == null || stage < 1 || stage > 6) {
            throw new RuntimeException("阶段值不正确");
        }
        Candidate origin = candidateMapper.selectById(id);
        if (origin == null) {
            throw new RuntimeException("候选人不存在或已删除");
        }
        if (origin.getStage() != null && origin.getStage() == Candidate.STAGE_HIRED) {
            throw new RuntimeException("该候选人已录用，阶段不能再调整");
        }
        if (stage == Candidate.STAGE_REJECTED && !StringUtils.hasText(reason)) {
            throw new RuntimeException("淘汰候选人必须填写原因");
        }
        Candidate update = new Candidate();
        update.setId(id);
        update.setStage(stage);
        update.setRejectReason(stage == Candidate.STAGE_REJECTED ? reason : null);
        update.setUpdateTime(LocalDateTime.now());
        return candidateMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long hire(Long id, HireDTO hireDTO) {
        Candidate candidate = candidateMapper.selectById(id);
        if (candidate == null) {
            throw new RuntimeException("候选人不存在或已删除");
        }
        if (candidate.getStage() != null && candidate.getStage() == Candidate.STAGE_HIRED) {
            throw new RuntimeException("该候选人已录用，不能重复录用");
        }
        if (candidate.getStage() != null && candidate.getStage() == Candidate.STAGE_REJECTED) {
            throw new RuntimeException("该候选人已淘汰，如需录用请先把阶段改回待录用");
        }

        JobRequisition req = requisitionMapper.selectById(candidate.getRequisitionId());
        if (req == null) {
            throw new RuntimeException("应聘的招聘需求不存在，无法录用");
        }
        int headcount = req.getHeadcount() == null ? 0 : req.getHeadcount();
        int hiredCount = req.getHiredCount() == null ? 0 : req.getHiredCount();
        if (headcount > 0 && hiredCount >= headcount) {
            throw new RuntimeException("该需求计划招 " + headcount + " 人、已招满，请先在需求页调整人数");
        }

        HireDTO dto = hireDTO == null ? new HireDTO() : hireDTO;
        LocalDate entryDate = dto.getEntryDate() != null ? dto.getEntryDate() : LocalDate.now();
        Long deptId = dto.getDeptId() != null ? dto.getDeptId() : req.getDeptId();
        String position = StringUtils.hasText(dto.getPosition()) ? dto.getPosition() : req.getPosition();

        // 1. 生成员工档案：状态直接是「2试用期」，入职日期按录用时填的
        Employee employee = new Employee();
        employee.setEmpNo(generateEmpNo());
        employee.setName(candidate.getName());
        employee.setGender(candidate.getGender());
        employee.setBirthday(candidate.getBirthday());
        employee.setPhone(candidate.getPhone());
        employee.setEmail(candidate.getEmail());
        employee.setDeptId(deptId);
        employee.setPosition(position);
        employee.setJobLevel(dto.getJobLevel());
        employee.setEntryDate(entryDate);
        employee.setWorkYears(0);
        employee.setEducation(candidate.getEducation());
        employee.setMajor(candidate.getMajor());
        // 招聘侧薪资按「K/月」录入（9.5 表示 9500 元），而 sys_employee.salary 存的是元，
        // 这里统一换算，否则员工列表会显示成 9.5 元
        BigDecimal salaryK = dto.getSalary() != null ? dto.getSalary() : candidate.getExpectSalary();
        employee.setSalary(salaryK == null ? null : salaryK.multiply(BigDecimal.valueOf(1000)));
        employee.setStatus(2);
        employee.setWorkLocation(StringUtils.hasText(dto.getWorkLocation())
                ? dto.getWorkLocation() : req.getWorkLocation());
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        employeeMapper.insert(employee);

        // 1.1 同步开通登录账号
        // 原先这一步缺失：录用后员工能出现在花名册里，但没有 sys_user，
        // 导致新人登不进系统，打卡接口还会报「当前账号未绑定员工档案」
        String username = generateUsername(employee.getEmpNo());
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setNickname(candidate.getName());
        user.setRole("user");
        user.setEmpId(employee.getId());
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        // 2. 建试用期记录，默认 3 个月
        int months = dto.getProbationMonths() == null ? 3 : dto.getProbationMonths();
        if (months <= 0 || months > 12) {
            throw new RuntimeException("试用期月数应在 1~12 之间");
        }
        Probation probation = new Probation();
        probation.setEmployeeId(employee.getId());
        probation.setCandidateId(candidate.getId());
        probation.setDeptId(deptId);
        probation.setPosition(position);
        probation.setProbationStart(entryDate);
        probation.setProbationEnd(entryDate.plusMonths(months));
        probation.setMentor(dto.getMentor());
        probation.setStatus(Probation.STATUS_PROBATION);
        probation.setCreateTime(LocalDateTime.now());
        probation.setUpdateTime(LocalDateTime.now());
        probationMapper.insert(probation);

        // 3. 回写候选人
        Candidate update = new Candidate();
        update.setId(candidate.getId());
        update.setStage(Candidate.STAGE_HIRED);
        update.setEmployeeId(employee.getId());
        update.setUpdateTime(LocalDateTime.now());
        candidateMapper.updateById(update);

        // 4. 需求已招人数 +1，招满自动置为已完成
        JobRequisition reqUpdate = new JobRequisition();
        reqUpdate.setId(req.getId());
        reqUpdate.setHiredCount(hiredCount + 1);
        if (headcount > 0 && hiredCount + 1 >= headcount) {
            reqUpdate.setStatus(JobRequisition.STATUS_DONE);
        }
        reqUpdate.setUpdateTime(LocalDateTime.now());
        requisitionMapper.updateById(reqUpdate);

        return employee.getId();
    }

    @Override
    public List<Map<String, Object>> stageFunnel() {
        List<Map<String, Object>> funnel = new ArrayList<>();
        for (int stage = 1; stage <= 6; stage++) {
            long count = candidateMapper.selectCount(new LambdaQueryWrapper<Candidate>()
                    .eq(Candidate::getStage, stage));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("stage", stage);
            item.put("name", STAGE_NAMES[stage - 1]);
            item.put("count", count);
            funnel.add(item);
        }
        return funnel;
    }

    /** 新增/编辑同一套必填口径：姓名、手机号、应聘需求、学历、工作年限、来源 */
    private void validate(Candidate c) {
        Validators.text(c.getName(), "姓名");
        Validators.text(c.getPhone(), "手机号");
        Validators.notNull(c.getRequisitionId(), "应聘岗位");
        Validators.text(c.getEducation(), "学历");
        Validators.notNull(c.getWorkYears(), "工作年限");
        Validators.text(c.getSource(), "来源渠道");
        if (c.getPhone() != null && !c.getPhone().matches("^[0-9+\\-\\s]{6,20}$")) {
            throw new RuntimeException("手机号格式不正确");
        }
        Validators.nonNegative(c.getWorkYears(), "工作年限");
        Validators.nonNegative(c.getExpectSalary(), "期望薪资");
    }

    /** 候选人编号 C + yyyyMMdd + 3 位序号 */
    private String generateCandidateNo() {
        String prefix = "C" + LocalDate.now().format(NO_DATE);
        long used = candidateMapper.selectCount(new LambdaQueryWrapper<Candidate>()
                .likeRight(Candidate::getCandidateNo, prefix));
        for (int i = 0; i < 200; i++) {
            String no = prefix + String.format("%03d", used + 1 + i);
            long exists = candidateMapper.selectCount(new LambdaQueryWrapper<Candidate>()
                    .eq(Candidate::getCandidateNo, no));
            if (exists == 0) {
                return no;
            }
        }
        return prefix + System.currentTimeMillis() % 100000;
    }

    /** 员工工号 EMP + 3 位序号，取含已删除记录的真实最大值，避开 uk_emp_no */
    private String generateEmpNo() {
        Integer max = employeeMapper.maxEmpNoSeqIncludeDeleted();
        int next = (max == null ? 0 : max) + 1;
        return String.format("EMP%03d", next);
    }

    /**
     * 录用时给新人生成登录账号：以工号为用户名，重名自动追加序号。
     * 初始密码与 UserServiceImpl 的默认密码保持一致（123456），首次登录后建议修改。
     */
    private String generateUsername(String empNo) {
        String base = StringUtils.hasText(empNo) ? empNo : "emp";
        String candidate = base;
        int suffix = 1;
        while (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, candidate)) > 0) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }
}
