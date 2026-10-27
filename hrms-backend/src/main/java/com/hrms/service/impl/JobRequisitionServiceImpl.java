package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.PageResult;
import com.hrms.dto.RecruitQueryDTO;
import com.hrms.entity.Candidate;
import com.hrms.entity.CandidateInterview;
import com.hrms.entity.Department;
import com.hrms.entity.JobRequisition;
import com.hrms.entity.Probation;
import com.hrms.mapper.CandidateInterviewMapper;
import com.hrms.mapper.CandidateMapper;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.JobRequisitionMapper;
import com.hrms.mapper.ProbationMapper;
import com.hrms.service.JobRequisitionService;
import com.hrms.util.CodeReleaser;
import com.hrms.util.SortUtil;
import com.hrms.util.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 招聘需求Service实现类
 */
@Service
public class JobRequisitionServiceImpl implements JobRequisitionService {

    private static final DateTimeFormatter NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 需求编号列长度 hr_recruit_requisition.req_no varchar(32) */
    private static final int REQ_NO_MAX_LEN = 32;

    /** 列表可排序字段白名单 */
    private static final Map<String, SortUtil.OrderCol> REQ_SORT = new HashMap<>();

    static {
        REQ_SORT.put("reqNo", SortUtil.OrderCol.natural("req_no"));
        REQ_SORT.put("position", SortUtil.OrderCol.natural("position"));
        REQ_SORT.put("headcount", SortUtil.OrderCol.plain("headcount"));
        REQ_SORT.put("urgency", SortUtil.OrderCol.plain("urgency"));
        REQ_SORT.put("status", SortUtil.OrderCol.plain("status"));
        REQ_SORT.put("publishDate", SortUtil.OrderCol.plain("publish_date"));
        REQ_SORT.put("deadline", SortUtil.OrderCol.plain("deadline"));
        REQ_SORT.put("createTime", SortUtil.OrderCol.plain("create_time"));
    }

    @Autowired
    private JobRequisitionMapper requisitionMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private CandidateMapper candidateMapper;

    @Autowired
    private CandidateInterviewMapper interviewMapper;

    @Autowired
    private ProbationMapper probationMapper;

    @Override
    public PageResult<JobRequisition> pageQuery(RecruitQueryDTO queryDTO) {
        Page<JobRequisition> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<JobRequisition> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(JobRequisition::getPosition, queryDTO.getKeyword())
                    .or().like(JobRequisition::getReqNo, queryDTO.getKeyword())
                    .or().like(JobRequisition::getApplicant, queryDTO.getKeyword()));
        }
        if (queryDTO.getDeptId() != null) {
            wrapper.eq(JobRequisition::getDeptId, queryDTO.getDeptId());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(JobRequisition::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getUrgency() != null) {
            wrapper.eq(JobRequisition::getUrgency, queryDTO.getUrgency());
        }

        String orderBy = SortUtil.build(queryDTO.getSortField(), queryDTO.getSortOrder(), REQ_SORT);
        if (orderBy != null) {
            wrapper.last(orderBy);
        } else {
            // 默认：在招的排前面，同状态内按紧急程度、发布时间倒序
            wrapper.orderByAsc(JobRequisition::getStatus)
                    .orderByDesc(JobRequisition::getUrgency)
                    .orderByDesc(JobRequisition::getCreateTime);
        }

        Page<JobRequisition> result = requisitionMapper.selectPage(page, wrapper);
        List<JobRequisition> records = result.getRecords();
        fillDeptName(records);
        fillCandidateStats(records);
        return new PageResult<>(result.getTotal(), records);
    }

    /** 批量补部门名称，避免 N+1 查询 */
    private void fillDeptName(List<JobRequisition> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> deptIds = records.stream()
                .map(JobRequisition::getDeptId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (deptIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = departmentMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));
        for (JobRequisition r : records) {
            if (r.getDeptId() != null) {
                r.setDeptName(nameMap.get(r.getDeptId()));
            }
        }
    }

    /**
     * 批量补候选人统计：一次查回这些需求下的所有候选人（只取需求 id + 阶段两列），
     * 在内存里按需求分组累加，避免按行 selectCount（每页 10 行就要打 10 次库）。
     * 投递 = 全部候选人；在用 = 阶段 1~4；淘汰 = 阶段 6；
     * 已录用沿用需求上的 hired_count（由录用动作维护，含手工补录，故不从候选人数反推）。
     */
    private void fillCandidateStats(List<JobRequisition> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> ids = records.stream()
                .map(JobRequisition::getId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, int[]> statMap = new HashMap<>();
        if (!ids.isEmpty()) {
            List<Candidate> candidates = candidateMapper.selectList(new LambdaQueryWrapper<Candidate>()
                    .select(Candidate::getRequisitionId, Candidate::getStage)
                    .in(Candidate::getRequisitionId, ids));
            for (Candidate c : candidates) {
                if (c.getRequisitionId() == null) {
                    continue;
                }
                int[] s = statMap.computeIfAbsent(c.getRequisitionId(), k -> new int[3]);
                s[0]++;
                if (c.getStage() != null && c.getStage() >= Candidate.STAGE_SCREENING
                        && c.getStage() <= Candidate.STAGE_OFFER) {
                    s[1]++;
                } else if (c.getStage() != null && c.getStage() == Candidate.STAGE_REJECTED) {
                    s[2]++;
                }
            }
        }

        for (JobRequisition r : records) {
            int[] s = statMap.get(r.getId());
            r.setCandidateCount(s == null ? 0 : s[0]);
            r.setActiveCount(s == null ? 0 : s[1]);
            r.setRejectedCount(s == null ? 0 : s[2]);
        }
    }

    @Override
    public JobRequisition getById(Long id) {
        JobRequisition requisition = requisitionMapper.selectById(id);
        if (requisition == null) {
            throw new RuntimeException("招聘需求不存在或已删除");
        }
        List<JobRequisition> one = new ArrayList<>();
        one.add(requisition);
        fillDeptName(one);
        fillCandidateStats(one);
        return requisition;
    }

    @Override
    public boolean save(JobRequisition requisition) {
        validate(requisition);
        if (requisition.getHiredCount() == null) {
            requisition.setHiredCount(0);
        }
        if (requisition.getStatus() == null) {
            requisition.setStatus(JobRequisition.STATUS_OPEN);
        }
        if (requisition.getUrgency() == null) {
            requisition.setUrgency(1);
        }
        if (requisition.getPublishDate() == null) {
            requisition.setPublishDate(LocalDate.now());
        }
        requisition.setId(null);
        requisition.setReqNo(generateReqNo());
        requisition.setCreateTime(LocalDateTime.now());
        requisition.setUpdateTime(LocalDateTime.now());
        return requisitionMapper.insert(requisition) > 0;
    }

    @Override
    public boolean update(JobRequisition requisition) {
        if (requisition.getId() == null) {
            throw new RuntimeException("招聘需求不存在");
        }
        JobRequisition origin = requisitionMapper.selectById(requisition.getId());
        if (origin == null) {
            throw new RuntimeException("招聘需求不存在或已删除");
        }
        validate(requisition);
        // 编号不随表单更新，防止前端改乱；已招人数只由录用动作维护，避免手工改出负数或超招
        requisition.setReqNo(origin.getReqNo());
        requisition.setHiredCount(origin.getHiredCount());
        requisition.setUpdateTime(LocalDateTime.now());
        return requisitionMapper.updateById(requisition) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        JobRequisition requisition = requisitionMapper.selectById(id);
        if (requisition == null) {
            throw new RuntimeException("招聘需求不存在或已删除");
        }
        LambdaQueryWrapper<Candidate> w = new LambdaQueryWrapper<>();
        w.eq(Candidate::getRequisitionId, id);
        long candidateCount = candidateMapper.selectCount(w);
        if (candidateCount > 0) {
            throw new RuntimeException("该需求下还有 " + candidateCount + " 位候选人，请先处理或关闭需求，不能直接删除");
        }
        // 让出唯一编码，否则同一时间点再建需求会撞 uk_req_no
        JobRequisition release = new JobRequisition();
        release.setId(id);
        release.setReqNo(CodeReleaser.release(requisition.getReqNo(), id, REQ_NO_MAX_LEN));
        release.setUpdateTime(LocalDateTime.now());
        requisitionMapper.updateById(release);

        return requisitionMapper.deleteById(id) > 0;
    }

    @Override
    public boolean changeStatus(Long id, Integer status) {
        if (status == null || status < 1 || status > 4) {
            throw new RuntimeException("状态值不正确");
        }
        JobRequisition origin = requisitionMapper.selectById(id);
        if (origin == null) {
            throw new RuntimeException("招聘需求不存在或已删除");
        }
        JobRequisition update = new JobRequisition();
        update.setId(id);
        update.setStatus(status);
        update.setUpdateTime(LocalDateTime.now());
        return requisitionMapper.updateById(update) > 0;
    }

    @Override
    public List<JobRequisition> listOpen() {
        LambdaQueryWrapper<JobRequisition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobRequisition::getStatus, JobRequisition.STATUS_OPEN)
                .orderByDesc(JobRequisition::getCreateTime);
        List<JobRequisition> list = requisitionMapper.selectList(wrapper);
        fillDeptName(list);
        return list;
    }

    @Override
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        LambdaQueryWrapper<JobRequisition> openWrapper = new LambdaQueryWrapper<>();
        openWrapper.eq(JobRequisition::getStatus, JobRequisition.STATUS_OPEN);
        List<JobRequisition> openList = requisitionMapper.selectList(openWrapper);

        int headcount = 0;
        int hiredCount = 0;
        for (JobRequisition r : openList) {
            headcount += r.getHeadcount() == null ? 0 : r.getHeadcount();
            hiredCount += r.getHiredCount() == null ? 0 : r.getHiredCount();
        }

        LambdaQueryWrapper<JobRequisition> allWrapper = new LambdaQueryWrapper<>();
        long totalRequisition = requisitionMapper.selectCount(allWrapper);

        long totalCandidate = candidateMapper.selectCount(new LambdaQueryWrapper<>());
        long hiredCandidate = candidateMapper.selectCount(new LambdaQueryWrapper<Candidate>()
                .eq(Candidate::getStage, Candidate.STAGE_HIRED));

        // 待面试：还没登记结果的面试场次
        long pendingInterview = interviewMapper.selectCount(new LambdaQueryWrapper<CandidateInterview>()
                .eq(CandidateInterview::getResult, CandidateInterview.RESULT_PENDING));

        long probationing = probationMapper.selectCount(new LambdaQueryWrapper<Probation>()
                .eq(Probation::getStatus, Probation.STATUS_PROBATION));
        long regularThisMonth = probationMapper.selectCount(new LambdaQueryWrapper<Probation>()
                .eq(Probation::getStatus, Probation.STATUS_REGULAR)
                .ge(Probation::getConfirmDate, LocalDate.now().withDayOfMonth(1)));

        stats.put("openRequisition", openList.size());
        stats.put("totalRequisition", totalRequisition);
        stats.put("headcount", headcount);
        stats.put("hiredCount", hiredCount);
        stats.put("gap", Math.max(0, headcount - hiredCount));
        stats.put("totalCandidate", totalCandidate);
        stats.put("hiredCandidate", hiredCandidate);
        stats.put("pendingInterview", pendingInterview);
        stats.put("probationing", probationing);
        stats.put("regularThisMonth", regularThisMonth);
        return stats;
    }

    /**
     * 新增/编辑同一套必填口径：岗位、部门、招聘人数、学历、经验、工作地点、期望到岗日期
     * 薪资范围选填，但填了就必须 min <= max，否则页面显示会出现"3K-1K"这种反向区间
     */
    private void validate(JobRequisition r) {
        Validators.text(r.getPosition(), "招聘岗位");
        Validators.notNull(r.getDeptId(), "需求部门");
        Validators.notNull(r.getHeadcount(), "招聘人数");
        if (r.getHeadcount() != null && r.getHeadcount() <= 0) {
            throw new RuntimeException("招聘人数必须大于 0");
        }
        Validators.text(r.getEducation(), "学历要求");
        Validators.text(r.getExperience(), "经验要求");
        Validators.text(r.getWorkLocation(), "工作地点");
        Validators.notNull(r.getDeadline(), "期望到岗日期");
        if (r.getSalaryMin() != null && r.getSalaryMax() != null
                && r.getSalaryMin().compareTo(r.getSalaryMax()) > 0) {
            throw new RuntimeException("薪资下限不能高于上限");
        }
        Validators.nonNegative(r.getSalaryMin(), "薪资下限");
        Validators.nonNegative(r.getSalaryMax(), "薪资上限");
    }

    /**
     * 生成需求编号 REQ + yyyyMMdd + 3 位序号
     * 逻辑删除的旧编号已被 CodeReleaser 改名为「原编号#DEL{id}」，不会与新编号相同，
     * 因此按"当天已有条数+1"再逐个探测空闲号即可；并发极小概率撞车由唯一索引兜底。
     */
    private String generateReqNo() {
        String prefix = "REQ" + LocalDate.now().format(NO_DATE);
        LambdaQueryWrapper<JobRequisition> w = new LambdaQueryWrapper<>();
        w.likeRight(JobRequisition::getReqNo, prefix);
        long used = requisitionMapper.selectCount(w);
        for (int i = 0; i < 200; i++) {
            String no = prefix + String.format("%03d", used + 1 + i);
            long exists = requisitionMapper.selectCount(new LambdaQueryWrapper<JobRequisition>()
                    .eq(JobRequisition::getReqNo, no));
            if (exists == 0) {
                return no;
            }
        }
        return prefix + System.currentTimeMillis() % 100000;
    }
}
