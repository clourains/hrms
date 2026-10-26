package com.hrms.service;

import com.hrms.dto.HireDTO;
import com.hrms.dto.PageResult;
import com.hrms.dto.RecruitQueryDTO;
import com.hrms.entity.Candidate;

import java.util.List;

/**
 * 招聘候选人Service接口
 */
public interface CandidateService {

    PageResult<Candidate> pageQuery(RecruitQueryDTO queryDTO);

    Candidate getById(Long id);

    boolean save(Candidate candidate);

    boolean update(Candidate candidate);

    boolean deleteById(Long id);

    /**
     * 手工调整候选人阶段（淘汰时必须填原因）
     */
    boolean changeStage(Long id, Integer stage, String reason);

    /**
     * 录用：生成员工档案 + 建立试用期记录 + 回写需求已招人数
     *
     * @return 新员工ID
     */
    Long hire(Long id, HireDTO hireDTO);

    /**
     * 各阶段人数分布（候选人页顶部漏斗）
     */
    List<java.util.Map<String, Object>> stageFunnel();
}
