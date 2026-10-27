package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.Attendance;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 考勤Mapper
 */
@Mapper
public interface AttendanceMapper extends BaseMapper<Attendance> {

    /**
     * 写入一条「非打卡」考勤记录（请假 / 出差）。
     *
     * 走原生 SQL 而不是 BaseMapper，原因有两个：
     * 1. sys_attendance 上有唯一键 uk_emp_date(emp_id, attend_date)，同一天重复插入会报
     *    DuplicateKeyException，所以要用 ON DUPLICATE KEY UPDATE 做 upsert；
     * 2. 全局逻辑删除（deleted）会过滤掉已删除的行，导致「查不到却插不进」，
     *    原生 SQL 可以同时处理这种情况（命中已删除行时直接复活并改写状态）。
     *
     * @return 影响行数
     */
    @Insert("INSERT INTO sys_attendance (emp_id, attend_date, status, remark, deleted, create_time, update_time) " +
            "VALUES (#{empId}, #{attendDate}, #{status}, #{remark}, 0, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE status = #{status}, remark = #{remark}, deleted = 0, update_time = NOW()")
    int upsertNonCheckIn(@Param("empId") Long empId,
                         @Param("attendDate") LocalDate attendDate,
                         @Param("status") Integer status,
                         @Param("remark") String remark);
}
