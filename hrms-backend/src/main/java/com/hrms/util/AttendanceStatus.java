package com.hrms.util;

/**
 * 考勤状态常量（唯一口径）
 *
 * 取值与 sys_attendance.status 的字段注释保持一致：
 * 1正常 2迟到 3早退 4缺勤 5请假 6出差 7迟到+早退
 *
 * 背景：历史上打卡代码用「0正常/1迟到/2早退/3迟到+早退」，与数据库注释和
 * 种子数据（1正常）冲突，导致薪资计算把正常出勤判成迟到。现统一到注释口径，
 * 「迟到+早退」在注释中没有对应取值，约定用 7 表示（迟到与早退各自计数）。
 */
public final class AttendanceStatus {

    /** 正常 */
    public static final int NORMAL = 1;
    /** 迟到 */
    public static final int LATE = 2;
    /** 早退 */
    public static final int EARLY_LEAVE = 3;
    /** 缺勤 */
    public static final int ABSENT = 4;
    /** 请假 */
    public static final int LEAVE = 5;
    /** 出差 */
    public static final int BUSINESS_TRIP = 6;
    /** 迟到且早退 */
    public static final int LATE_AND_EARLY = 7;

    private AttendanceStatus() {
    }

    /** 是否算迟到 */
    public static boolean isLate(Integer status) {
        return status != null && (status == LATE || status == LATE_AND_EARLY);
    }

    /** 是否算早退 */
    public static boolean isEarlyLeave(Integer status) {
        return status != null && (status == EARLY_LEAVE || status == LATE_AND_EARLY);
    }

    /** 是否算缺勤 */
    public static boolean isAbsent(Integer status) {
        return status != null && status == ABSENT;
    }

    /** 是否算请假（不算缺勤，也不参与出勤天数） */
    public static boolean isLeave(Integer status) {
        return status != null && status == LEAVE;
    }

    /** 是否算出差（出差视为正常出勤） */
    public static boolean isBusinessTrip(Integer status) {
        return status != null && status == BUSINESS_TRIP;
    }

    /** 是否有实际出勤（迟到/早退也算出勤，请假不算） */
    public static boolean isPresent(Integer status) {
        return status != null && status != LEAVE;
    }
}
