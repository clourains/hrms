-- V12 统一考勤状态口径
--
-- 背景：sys_attendance.status 的字段注释是「1正常 2迟到 3早退 4缺勤 5请假 6出差」，
-- 但打卡代码（AttendanceServiceImpl）历史上写的是「0正常 1迟到 2早退 3迟到+早退」，
-- 两套口径冲突，导致薪资计算（SalaryServiceImpl）把大量「正常」记录判成迟到扣款。
--
-- 本脚本做两件事：
--   1. 更新字段注释，补上「7迟到+早退」（迟到与早退各自单独计数，所以需要一个组合值）
--   2. 订正 status = 0 的历史数据 —— 0 在新口径里不存在，只可能是旧打卡代码写入的
--      「正常」，可以无歧义地订正为 1。
--
-- 注意：status 为 1/2/3 的记录无法区分来源（种子数据用的是新口径，旧打卡写的也是
-- 这几个值），因此这里不做批量映射，避免把种子数据改坏。如果你的库里确认有大量
-- 「通过页面打卡产生」的历史记录，请在执行本脚本后单独运行
-- db/fix_attendance_status_legacy.sql（该脚本内有判断方法和使用说明）。
--
-- 顺带订正性别口径：sys_employee.gender 的注释是「0女 1男」，Excel 导入也按女=0 落库，
-- 但员工编辑弹窗里「女」的 radio 一直提交的是 2，导致这类员工在性别分布图里既不算男
-- 也不算女（女性人数恒为 0）。把 2 统一订正为 0。

ALTER TABLE `sys_attendance`
    MODIFY COLUMN `status` tinyint DEFAULT '1'
    COMMENT '状态 1正常 2迟到 3早退 4缺勤 5请假 6出差 7迟到+早退';

-- 旧口径的「0正常」在新口径里不存在，可无歧义订正
UPDATE `sys_attendance` SET `status` = 1 WHERE `status` = 0;

-- 性别：2 不是合法取值（注释为 0女 1男），订正为 0 女
UPDATE `sys_employee` SET `gender` = 0 WHERE `gender` = 2;
