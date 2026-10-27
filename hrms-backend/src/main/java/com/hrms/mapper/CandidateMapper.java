package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.Candidate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 候选人Mapper
 */
@Mapper
public interface CandidateMapper extends BaseMapper<Candidate> {

}
