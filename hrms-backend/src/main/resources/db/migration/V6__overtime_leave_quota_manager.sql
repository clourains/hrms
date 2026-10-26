-- P4-1 加班管理：加班申请（走通用审批流，通过后计入薪资加班费）
CREATE TABLE IF NOT EXISTS sys_overtime (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    emp_id BIGINT NOT NULL COMMENT '员工ID',
    overtime_date DATE NOT NULL COMMENT '加班日期',
    hours DECIMAL(4,1) NOT NULL COMMENT '加班时长(小时)',
    reason VARCHAR(500) DEFAULT NULL COMMENT '加班事由',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0审批中 1已通过 2已驳回 3已撤销',
    instance_id BIGINT DEFAULT NULL COMMENT '关联审批实例ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ot_emp (emp_id, overtime_date),
    KEY idx_ot_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加班申请';

-- P4-2 请假额度池：按年按类型管理（当前启用年假）
CREATE TABLE IF NOT EXISTS sys_leave_quota (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    emp_id BIGINT NOT NULL COMMENT '员工ID',
    year INT NOT NULL COMMENT '年度',
    quota_type VARCHAR(20) NOT NULL DEFAULT '年假' COMMENT '额度类型',
    total_days DECIMAL(5,1) NOT NULL DEFAULT 0 COMMENT '总额度(天)',
    used_days DECIMAL(5,1) NOT NULL DEFAULT 0 COMMENT '已用额度(天)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_emp_year_type (emp_id, year, quota_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假额度';

-- 为在职员工初始化 2026 年年假额度（10天/年）
INSERT IGNORE INTO sys_leave_quota (emp_id, year, quota_type, total_days, used_days)
SELECT id, 2026, '年假', 10, 0 FROM sys_employee WHERE status = 1 AND deleted = 0;

-- P4-3 部门数据权限演示账号：manager/123456（MD5 首登自动升级 BCrypt），
-- 绑定一名在职员工，其部门即数据可见范围
INSERT INTO sys_user (username, password, nickname, role, emp_id, status)
SELECT 'manager', 'e10adc3949ba59abbe56e057f20f883e', e.name, 'manager', e.id, 1
FROM sys_employee e
WHERE e.status = 1 AND e.dept_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'manager')
ORDER BY e.id DESC
LIMIT 1;
