package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.entity.Performance;
import com.hrms.mapper.PerformanceMapper;
import com.hrms.service.PerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 绩效Service实现类
 */
@Service
public class PerformanceServiceImpl implements PerformanceService {

    @Autowired
    private PerformanceMapper performanceMapper;

    @Override
    public Performance getById(Long id) {
        return performanceMapper.selectById(id);
    }

    @Override
    public List<Performance> listByEmpId(Long empId) {
        LambdaQueryWrapper<Performance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Performance::getEmpId, empId)
                .orderByDesc(Performance::getPeriod);
        return performanceMapper.selectList(queryWrapper);
    }

    @Override
    public List<Performance> listByPeriod(String period) {
        LambdaQueryWrapper<Performance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Performance::getPeriod, period)
                .orderByAsc(Performance::getEmpId);
        return performanceMapper.selectList(queryWrapper);
    }

    @Override
    public boolean evaluate(Performance performance) {
        // 计算总分
        calculateTotalScore(performance);
        // 评定等级
        determineGrade(performance);
        performance.setUpdateTime(LocalDateTime.now());

        if (performance.getId() == null) {
            performance.setCreateTime(LocalDateTime.now());
            return performanceMapper.insert(performance) > 0;
        } else {
            return performanceMapper.updateById(performance) > 0;
        }
    }

    @Override
    public boolean save(Performance performance) {
        // P042：同员工同周期防重（逻辑删除的记录不算），避免裸 SQL 唯一键报错
        Long empId = performance.getEmpId();
        String period = performance.getPeriod();
        if (empId != null && period != null) {
            LambdaQueryWrapper<Performance> dupWrapper = new LambdaQueryWrapper<>();
            dupWrapper.eq(Performance::getEmpId, empId).eq(Performance::getPeriod, period);
            if (performanceMapper.selectCount(dupWrapper) > 0) {
                throw new RuntimeException("该员工本考核周期绩效记录已存在，请直接编辑原记录");
            }
        }
        calculateTotalScore(performance);
        determineGrade(performance);
        performance.setCreateTime(LocalDateTime.now());
        performance.setUpdateTime(LocalDateTime.now());
        return performanceMapper.insert(performance) > 0;
    }

    @Override
    public boolean update(Performance performance) {
        calculateTotalScore(performance);
        determineGrade(performance);
        performance.setUpdateTime(LocalDateTime.now());
        return performanceMapper.updateById(performance) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return performanceMapper.deleteById(id) > 0;
    }

    @Override
    public Map<String, Object> statistics(String period) {
        List<Performance> list = listByPeriod(period);

        Map<String, Object> result = new HashMap<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        int gradeACount = 0;
        int gradeBCount = 0;
        int gradeCCount = 0;
        int gradeDCount = 0;

        for (Performance perf : list) {
            if (perf.getTotalScore() != null) {
                totalScore = totalScore.add(perf.getTotalScore());
            }
            String grade = perf.getGrade();
            if ("A".equals(grade)) {
                gradeACount++;
            } else if ("B".equals(grade)) {
                gradeBCount++;
            } else if ("C".equals(grade)) {
                gradeCCount++;
            } else if ("D".equals(grade)) {
                gradeDCount++;
            }
        }

        BigDecimal avgScore = list.size() > 0
                ? totalScore.divide(BigDecimal.valueOf(list.size()), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;

        result.put("totalCount", list.size());
        result.put("avgScore", avgScore);
        result.put("gradeACount", gradeACount);
        result.put("gradeBCount", gradeBCount);
        result.put("gradeCCount", gradeCCount);
        result.put("gradeDCount", gradeDCount);

        return result;
    }

    /**
     * 计算总分
     */
    private void calculateTotalScore(Performance performance) {
        BigDecimal total = BigDecimal.ZERO;
        if (performance.getWorkQuality() != null) {
            total = total.add(performance.getWorkQuality());
        }
        if (performance.getWorkEfficiency() != null) {
            total = total.add(performance.getWorkEfficiency());
        }
        if (performance.getWorkAttitude() != null) {
            total = total.add(performance.getWorkAttitude());
        }
        if (performance.getTeamwork() != null) {
            total = total.add(performance.getTeamwork());
        }
        if (performance.getInnovation() != null) {
            total = total.add(performance.getInnovation());
        }
        performance.setTotalScore(total);
    }

    /**
     * 评定等级
     */
    private void determineGrade(Performance performance) {
        BigDecimal totalScore = performance.getTotalScore();
        if (totalScore == null) {
            return;
        }
        // 假设满分100分，五个维度各20分
        if (totalScore.compareTo(BigDecimal.valueOf(90)) >= 0) {
            performance.setGrade("A");
        } else if (totalScore.compareTo(BigDecimal.valueOf(80)) >= 0) {
            performance.setGrade("B");
        } else if (totalScore.compareTo(BigDecimal.valueOf(60)) >= 0) {
            performance.setGrade("C");
        } else {
            performance.setGrade("D");
        }
    }
}
