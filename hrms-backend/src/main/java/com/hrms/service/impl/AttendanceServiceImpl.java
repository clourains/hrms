package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.entity.Attendance;
import com.hrms.mapper.AttendanceMapper;
import com.hrms.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考勤Service实现类
 */
@Service
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Override
    public Attendance checkIn(Long empId) {
        LocalDate today = LocalDate.now();

        // 检查今日是否已打卡
        LambdaQueryWrapper<Attendance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Attendance::getEmpId, empId)
                .eq(Attendance::getAttendDate, today);
        Attendance attendance = attendanceMapper.selectOne(queryWrapper);

        if (attendance != null && attendance.getCheckInTime() != null) {
            throw new RuntimeException("今日已上班打卡");
        }

        LocalDateTime now = LocalDateTime.now();

        if (attendance == null) {
            // 新建打卡记录
            attendance = new Attendance();
            attendance.setEmpId(empId);
            attendance.setAttendDate(today);
            attendance.setCheckInTime(now);
            attendance.setCreateTime(now);
            attendance.setUpdateTime(now);
            // 判断是否迟到（假设9:00上班）
            if (now.getHour() >= 9) {
                attendance.setStatus(1); // 迟到
            } else {
                attendance.setStatus(0); // 正常
            }
            attendanceMapper.insert(attendance);
        } else {
            // 更新打卡记录
            attendance.setCheckInTime(now);
            attendance.setUpdateTime(now);
            if (now.getHour() >= 9) {
                attendance.setStatus(1); // 迟到
            }
            attendanceMapper.updateById(attendance);
        }

        return attendance;
    }

    @Override
    public Attendance checkOut(Long empId) {
        LocalDate today = LocalDate.now();

        // 查询今日打卡记录
        LambdaQueryWrapper<Attendance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Attendance::getEmpId, empId)
                .eq(Attendance::getAttendDate, today);
        Attendance attendance = attendanceMapper.selectOne(queryWrapper);

        if (attendance == null) {
            throw new RuntimeException("请先进行上班打卡");
        }

        if (attendance.getCheckOutTime() != null) {
            throw new RuntimeException("今日已下班打卡");
        }

        LocalDateTime now = LocalDateTime.now();
        attendance.setCheckOutTime(now);
        attendance.setUpdateTime(now);

        // 计算工作时长
        if (attendance.getCheckInTime() != null) {
            Duration duration = Duration.between(attendance.getCheckInTime(), now);
            double hours = duration.toMinutes() / 60.0;
            attendance.setWorkHours(BigDecimal.valueOf(hours).setScale(2, BigDecimal.ROUND_HALF_UP));
        }

        // 判断是否早退（假设18:00下班）
        if (now.getHour() < 18) {
            if (attendance.getStatus() == 0) {
                attendance.setStatus(2); // 早退
            } else if (attendance.getStatus() == 1) {
                attendance.setStatus(3); // 迟到+早退
            }
        }

        attendanceMapper.updateById(attendance);
        return attendance;
    }

    @Override
    public Attendance getById(Long id) {
        return attendanceMapper.selectById(id);
    }

    @Override
    public List<Attendance> listByEmpIdAndMonth(Long empId, String month) {
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        LambdaQueryWrapper<Attendance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Attendance::getEmpId, empId)
                .between(Attendance::getAttendDate, startDate, endDate)
                .orderByAsc(Attendance::getAttendDate);

        return attendanceMapper.selectList(queryWrapper);
    }

    @Override
    public Map<String, Object> monthlyStatistics(Long empId, String month) {
        List<Attendance> list = listByEmpIdAndMonth(empId, month);

        Map<String, Object> result = new HashMap<>();
        int normalCount = 0;     // 正常
        int lateCount = 0;       // 迟到
        int earlyLeaveCount = 0; // 早退
        int absentCount = 0;     // 缺勤
        BigDecimal totalWorkHours = BigDecimal.ZERO;

        for (Attendance att : list) {
            Integer status = att.getStatus();
            if (status == null || status == 0) {
                normalCount++;
            } else if (status == 1) {
                lateCount++;
            } else if (status == 2) {
                earlyLeaveCount++;
            } else if (status == 3) {
                lateCount++;
                earlyLeaveCount++;
            }
            if (att.getWorkHours() != null) {
                totalWorkHours = totalWorkHours.add(att.getWorkHours());
            }
        }

        result.put("totalDays", list.size());
        result.put("normalCount", normalCount);
        result.put("lateCount", lateCount);
        result.put("earlyLeaveCount", earlyLeaveCount);
        result.put("absentCount", absentCount);
        result.put("totalWorkHours", totalWorkHours.setScale(2, BigDecimal.ROUND_HALF_UP));

        return result;
    }

    @Override
    public boolean save(Attendance attendance) {
        attendance.setCreateTime(LocalDateTime.now());
        attendance.setUpdateTime(LocalDateTime.now());
        return attendanceMapper.insert(attendance) > 0;
    }

    @Override
    public boolean update(Attendance attendance) {
        attendance.setUpdateTime(LocalDateTime.now());
        return attendanceMapper.updateById(attendance) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return attendanceMapper.deleteById(id) > 0;
    }
}
