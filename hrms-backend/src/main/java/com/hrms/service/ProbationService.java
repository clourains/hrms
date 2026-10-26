package com.hrms.service;

import com.hrms.dto.PageResult;
import com.hrms.dto.RecruitQueryDTO;
import com.hrms.entity.Probation;

import java.time.LocalDate;
import java.util.Map;

/**
 * 试用期转正Service接口
 */
public interface ProbationService {

    PageResult<Probation> pageQuery(RecruitQueryDTO queryDTO);

    Probation getById(Long id);

    boolean save(Probation probation);

    boolean update(Probation probation);

    boolean deleteById(Long id);

    /**
     * 转正：试用期记录置为已转正，员工状态 2试用期 -> 1在职
     */
    boolean confirm(Long id, Integer score, String evaluation);

    /**
     * 不予转正：记录置为未通过，员工状态置为 0离职
     */
    boolean fail(Long id, String evaluation);

    /**
     * 延长试用期：只改结束日期，记录仍为试用中
     */
    boolean extend(Long id, LocalDate newEnd, String evaluation);

    /**
     * 试用期概览：试用中、N 天内到期、已逾期、本月已转正
     *
     * @param expireDays 到期提醒的提前天数，为空时取默认值 30
     */
    Map<String, Object> statistics(Integer expireDays);
}
