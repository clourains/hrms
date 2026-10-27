package com.hrms.service;

import com.hrms.dto.PageResult;
import com.hrms.dto.RecruitQueryDTO;
import com.hrms.entity.CandidateInterview;

import java.util.List;

/**
 * 面试安排Service接口
 */
public interface CandidateInterviewService {

    PageResult<CandidateInterview> pageQuery(RecruitQueryDTO queryDTO);

    /**
     * 某个候选人的全部面试记录（按时间倒序）
     */
    List<CandidateInterview> listByCandidate(Long candidateId);

    CandidateInterview getById(Long id);

    boolean save(CandidateInterview interview);

    boolean update(CandidateInterview interview);

    boolean deleteById(Long id);

    /**
     * 登记面试结果，并按结果自动推进候选人阶段
     *
     * 通过：初试 -> 复试、复试/终试 -> 待录用
     * 不通过 / 爽约：候选人置为已淘汰
     */
    boolean submitResult(Long id, Integer result, Integer score, String comment);
}
