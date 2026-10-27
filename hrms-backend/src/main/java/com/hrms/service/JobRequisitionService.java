package com.hrms.service;

import com.hrms.dto.PageResult;
import com.hrms.dto.RecruitQueryDTO;
import com.hrms.entity.JobRequisition;

import java.util.List;
import java.util.Map;

/**
 * 招聘需求Service接口
 */
public interface JobRequisitionService {

    PageResult<JobRequisition> pageQuery(RecruitQueryDTO queryDTO);

    JobRequisition getById(Long id);

    boolean save(JobRequisition requisition);

    boolean update(JobRequisition requisition);

    boolean deleteById(Long id);

    /**
     * 变更需求状态（招聘中/暂停/完成/关闭）
     */
    boolean changeStatus(Long id, Integer status);

    /**
     * 在招需求下拉（候选人登记时选岗位用）
     */
    List<JobRequisition> listOpen();

    /**
     * 招聘看板统计：需求数、在招人数、候选人分布、待面试、待转正
     */
    Map<String, Object> statistics();
}
