package com.hrms.service;

import com.hrms.entity.Attendance;

import java.util.List;
import java.util.Map;

/**
 * 考勤Service接口
 */
public interface AttendanceService {

    /**
     * 上班打卡
     *
     * @param empId 员工ID
     * @return 打卡记录
     */
    Attendance checkIn(Long empId);

    /**
     * 下班打卡
     *
     * @param empId 员工ID
     * @return 打卡记录
     */
    Attendance checkOut(Long empId);

    /**
     * 根据ID获取考勤记录
     *
     * @param id 考勤ID
     * @return 考勤记录
     */
    Attendance getById(Long id);

    /**
     * 获取员工考勤列表
     *
     * @param empId 员工ID
     * @param month 月份（格式：yyyy-MM）
     * @return 考勤列表
     */
    List<Attendance> listByEmpIdAndMonth(Long empId, String month);

    /**
     * 月度考勤统计
     *
     * @param empId 员工ID
     * @param month 月份（格式：yyyy-MM）
     * @return 统计结果
     */
    Map<String, Object> monthlyStatistics(Long empId, String month);

    /**
     * 新增考勤记录
     *
     * @param attendance 考勤记录
     * @return 是否成功
     */
    boolean save(Attendance attendance);

    /**
     * 更新考勤记录
     *
     * @param attendance 考勤记录
     * @return 是否成功
     */
    boolean update(Attendance attendance);

    /**
     * 根据ID删除考勤记录
     *
     * @param id 考勤ID
     * @return 是否成功
     */
    boolean deleteById(Long id);
}
