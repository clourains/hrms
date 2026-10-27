package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.PageResult;
import com.hrms.dto.RecruitQueryDTO;
import com.hrms.entity.Candidate;
import com.hrms.entity.CandidateInterview;
import com.hrms.entity.JobRequisition;
import com.hrms.mapper.CandidateInterviewMapper;
import com.hrms.mapper.CandidateMapper;
import com.hrms.mapper.JobRequisitionMapper;
import com.hrms.service.CandidateInterviewService;
import com.hrms.util.SortUtil;
import com.hrms.util.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 面试安排Service实现类
 */
@Service
public class CandidateInterviewServiceImpl implements CandidateInterviewService {

    private static final Map<String, SortUtil.OrderCol> INTERVIEW_SORT = new HashMap<>();

    static {
        INTERVIEW_SORT.put("interviewTime", SortUtil.OrderCol.plain("interview_time"));
        INTERVIEW_SORT.put("round", SortUtil.OrderCol.plain("round"));
        INTERVIEW_SORT.put("result", SortUtil.OrderCol.plain("result"));
        INTERVIEW_SORT.put("score", SortUtil.OrderCol.plain("score"));
        INTERVIEW_SORT.put("createTime", SortUtil.OrderCol.plain("create_time"));
    }

    @Autowired
    private CandidateInterviewMapper interviewMapper;

    @Autowired
    private CandidateMapper candidateMapper;

    @Autowired
    private JobRequisitionMapper requisitionMapper;

    @Override
    public PageResult<CandidateInterview> pageQuery(RecruitQueryDTO queryDTO) {
        Page<CandidateInterview> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<CandidateInterview> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String keyword = queryDTO.getKeyword();
            // 关键字也支持按候选人姓名搜：先查姓名命中的候选人 id，再 union 进来。
            // 不用字符串拼 SQL，避免把用户输入直接塞进 inSql 造成注入。
            final List<Long> nameMatchedIds = candidateMapper.selectList(
                            new LambdaQueryWrapper<Candidate>()
                                    .select(Candidate::getId)
                                    .like(Candidate::getName, keyword))
                    .stream().map(Candidate::getId).collect(Collectors.toList());
            wrapper.and(w -> {
                w.like(CandidateInterview::getInterviewer, keyword)
                        .or().like(CandidateInterview::getLocation, keyword);
                if (!nameMatchedIds.isEmpty()) {
                    w.or().in(CandidateInterview::getCandidateId, nameMatchedIds);
                }
            });
        }
        if (queryDTO.getCandidateId() != null) {
            wrapper.eq(CandidateInterview::getCandidateId, queryDTO.getCandidateId());
        }
        if (queryDTO.getRequisitionId() != null) {
            wrapper.eq(CandidateInterview::getRequisitionId, queryDTO.getRequisitionId());
        }
        if (queryDTO.getResult() != null) {
            wrapper.eq(CandidateInterview::getResult, queryDTO.getResult());
        }
        if (queryDTO.getRound() != null) {
            wrapper.eq(CandidateInterview::getRound, queryDTO.getRound());
        }

        String orderBy = SortUtil.build(queryDTO.getSortField(), queryDTO.getSortOrder(), INTERVIEW_SORT);
        if (orderBy != null) {
            wrapper.last(orderBy);
        } else {
            // 默认：待面试的排前面，其余按面试时间倒序
            wrapper.orderByAsc(CandidateInterview::getResult)
                    .orderByDesc(CandidateInterview::getInterviewTime);
        }

        Page<CandidateInterview> result = interviewMapper.selectPage(page, wrapper);
        fillExtras(result.getRecords());
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    /** 补候选人姓名/电话/岗位/阶段，列表不用再点进详情 */
    private void fillExtras(List<CandidateInterview> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> candidateIds = records.stream().map(CandidateInterview::getCandidateId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (candidateIds.isEmpty()) {
            return;
        }
        Map<Long, Candidate> candidateMap = candidateMapper.selectBatchIds(candidateIds).stream()
                .collect(Collectors.toMap(Candidate::getId, c -> c, (a, b) -> a));
        List<Long> reqIds = candidateMap.values().stream().map(Candidate::getRequisitionId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> positionMap = reqIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : requisitionMapper.selectBatchIds(reqIds).stream()
                        .collect(Collectors.toMap(JobRequisition::getId, JobRequisition::getPosition, (a, b) -> a));

        for (CandidateInterview item : records) {
            Candidate candidate = candidateMap.get(item.getCandidateId());
            if (candidate != null) {
                item.setCandidateName(candidate.getName());
                item.setCandidatePhone(candidate.getPhone());
                item.setCandidateStage(candidate.getStage());
                item.setRequisitionPosition(positionMap.get(candidate.getRequisitionId()));
            }
        }
    }

    @Override
    public List<CandidateInterview> listByCandidate(Long candidateId) {
        List<CandidateInterview> list = interviewMapper.selectList(
                new LambdaQueryWrapper<CandidateInterview>()
                        .eq(CandidateInterview::getCandidateId, candidateId)
                        .orderByDesc(CandidateInterview::getInterviewTime));
        fillExtras(list);
        return list;
    }

    @Override
    public CandidateInterview getById(Long id) {
        CandidateInterview interview = interviewMapper.selectById(id);
        if (interview == null) {
            throw new RuntimeException("面试记录不存在或已删除");
        }
        return interview;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(CandidateInterview interview) {
        validate(interview);
        Candidate candidate = candidateMapper.selectById(interview.getCandidateId());
        if (candidate == null) {
            throw new RuntimeException("候选人不存在或已删除");
        }
        if (candidate.getStage() != null && candidate.getStage() == Candidate.STAGE_HIRED) {
            throw new RuntimeException("该候选人已录用，不需要再安排面试");
        }
        interview.setId(null);
        if (interview.getRequisitionId() == null) {
            interview.setRequisitionId(candidate.getRequisitionId());
        }
        if (interview.getResult() == null) {
            interview.setResult(CandidateInterview.RESULT_PENDING);
        }
        if (interview.getDuration() == null) {
            interview.setDuration(60);
        }
        interview.setCreateTime(LocalDateTime.now());
        interview.setUpdateTime(LocalDateTime.now());
        boolean saved = interviewMapper.insert(interview) > 0;

        // 安排初试就把候选人从「待筛选」推进到「初试」，列表上能看到进度
        if (saved && candidate.getStage() != null && candidate.getStage() == Candidate.STAGE_SCREENING
                && interview.getRound() != null && interview.getRound() == 1) {
            Candidate update = new Candidate();
            update.setId(candidate.getId());
            update.setStage(Candidate.STAGE_FIRST);
            update.setUpdateTime(LocalDateTime.now());
            candidateMapper.updateById(update);
        }
        return saved;
    }

    @Override
    public boolean update(CandidateInterview interview) {
        if (interview.getId() == null) {
            throw new RuntimeException("面试记录不存在");
        }
        CandidateInterview origin = interviewMapper.selectById(interview.getId());
        if (origin == null) {
            throw new RuntimeException("面试记录不存在或已删除");
        }
        validate(interview);
        // 已有结果由「登记结果」维护，普通编辑不改，避免绕过候选人阶段推进
        interview.setResult(origin.getResult());
        interview.setCandidateId(origin.getCandidateId());
        interview.setRequisitionId(origin.getRequisitionId());
        interview.setUpdateTime(LocalDateTime.now());
        return interviewMapper.updateById(interview) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        CandidateInterview origin = interviewMapper.selectById(id);
        if (origin == null) {
            throw new RuntimeException("面试记录不存在或已删除");
        }
        return interviewMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitResult(Long id, Integer result, Integer score, String comment) {
        if (result == null || result < 1 || result > 3) {
            throw new RuntimeException("请选择面试结果（通过/未通过/爽约）");
        }
        CandidateInterview interview = interviewMapper.selectById(id);
        if (interview == null) {
            throw new RuntimeException("面试记录不存在或已删除");
        }
        if (interview.getResult() != null && interview.getResult() != CandidateInterview.RESULT_PENDING
                && interview.getResult().equals(result)) {
            throw new RuntimeException("该场面试已登记为相同结果，无需重复提交");
        }
        if (score != null && (score < 0 || score > 100)) {
            throw new RuntimeException("面试评分应在 0~100 之间");
        }

        CandidateInterview update = new CandidateInterview();
        update.setId(id);
        update.setResult(result);
        update.setScore(score);
        update.setComment(comment);
        update.setUpdateTime(LocalDateTime.now());
        boolean ok = interviewMapper.updateById(update) > 0;

        Candidate candidate = candidateMapper.selectById(interview.getCandidateId());
        if (ok && candidate != null && candidate.getStage() != null
                && candidate.getStage() != Candidate.STAGE_HIRED
                && candidate.getStage() != Candidate.STAGE_REJECTED) {
            Candidate candUpdate = new Candidate();
            candUpdate.setId(candidate.getId());
            candUpdate.setUpdateTime(LocalDateTime.now());
            if (result == CandidateInterview.RESULT_PASS) {
                int round = interview.getRound() == null ? 1 : interview.getRound();
                // 初试通过进入复试，复试/终试通过进入待录用（发 offer）
                candUpdate.setStage(round >= 2 ? Candidate.STAGE_OFFER : Candidate.STAGE_SECOND);
            } else {
                candUpdate.setStage(Candidate.STAGE_REJECTED);
                candUpdate.setRejectReason(result == CandidateInterview.RESULT_ABSENT
                        ? "第 " + interview.getRound() + " 轮面试爽约" : "第 " + interview.getRound() + " 轮面试未通过");
            }
            candidateMapper.updateById(candUpdate);
        }
        return ok;
    }

    /** 新增/编辑同一套必填口径：候选人、轮次、形式、面试时间、时长、面试官、地点 */
    private void validate(CandidateInterview i) {
        Validators.notNull(i.getCandidateId(), "候选人");
        Validators.notNull(i.getRound(), "面试轮次");
        Validators.notNull(i.getInterviewType(), "面试形式");
        Validators.notNull(i.getInterviewTime(), "面试时间");
        Validators.notNull(i.getDuration(), "面试时长");
        Validators.text(i.getInterviewer(), "面试官");
        Validators.text(i.getLocation(), "面试地点");
        Validators.nonNegative(i.getScore(), "面试评分");
    }
}
