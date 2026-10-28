-- 入职登记：独立于员工档案的登记台账
-- 先登记候选人/新员工信息，再「关联已有档案」或「一键新建档案」，
-- employee_id 为空表示还没落档案，便于 HR 在入职前先收资料。

CREATE TABLE IF NOT EXISTS hr_employee_onboard (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    onboard_no VARCHAR(32) NOT NULL COMMENT '登记编号',
    employee_id BIGINT COMMENT '关联的员工档案ID（为空表示待关联）',
    name VARCHAR(64) NOT NULL COMMENT '姓名',
    gender TINYINT COMMENT '性别 1男 2女',
    phone VARCHAR(32) COMMENT '手机号',
    dept_id BIGINT COMMENT '部门ID',
    dept_name VARCHAR(64) COMMENT '部门名称（冗余）',
    position VARCHAR(128) COMMENT '岗位',
    entry_date DATE NOT NULL COMMENT '入职日期',
    probation_months INT DEFAULT 3 COMMENT '试用期月数',
    source VARCHAR(32) COMMENT '来源：招聘录用/内推/校招/其他',
    status TINYINT DEFAULT 0 COMMENT '0待关联档案 1已关联档案',
    remark VARCHAR(255) COMMENT '备注',
    create_by VARCHAR(64) COMMENT '登记人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_onboard_no (onboard_no),
    INDEX idx_onboard_emp (employee_id),
    INDEX idx_onboard_date (entry_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '员工入职登记台账';

-- 演示数据：一条已关联档案、一条待关联档案
INSERT INTO hr_employee_onboard
    (onboard_no, employee_id, name, gender, phone, dept_id, dept_name, position, entry_date,
     probation_months, source, status, create_by)
SELECT 'RD20261001', e.id, e.name, e.gender, e.phone, e.dept_id,
       (SELECT d.dept_name FROM sys_department d WHERE d.id = e.dept_id),
       e.position, e.entry_date, 3, '招聘录用', 1, 'admin'
FROM sys_employee e
WHERE e.deleted = 0 AND e.status <> 0 AND e.id NOT IN (1)
ORDER BY e.id
LIMIT 1;

INSERT INTO hr_employee_onboard
    (onboard_no, name, gender, phone, dept_id, dept_name, position, entry_date,
     probation_months, source, status, remark, create_by)
SELECT 'RD20261002', '待入职·林可', 2, '13800001234', d.id, d.dept_name, '外贸业务员',
       '2026-11-03', 3, '招聘录用', 0, '已发录用通知，等待报到提交资料', 'admin'
FROM sys_department d
WHERE d.deleted = 0
ORDER BY d.id
LIMIT 1;
