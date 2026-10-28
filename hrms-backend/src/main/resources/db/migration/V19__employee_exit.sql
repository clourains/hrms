-- 入离管理：离职 / 辞退单据表
-- 一张表统一承载「主动离职」和「辞退」两类流程：
--   exit_kind 区分类别（列表按类别拆两个页面），exit_type 细分原因；
--   审批通过(1) 后由管理员点「生效」才真正改员工档案，避免审批中就把人置为离职。
-- 员工档案只改 status/leave_date，历史数据全部保留，便于离职率与辞退率统计。

CREATE TABLE IF NOT EXISTS hr_employee_exit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exit_no VARCHAR(32) NOT NULL COMMENT '单据编号',
    exit_kind TINYINT NOT NULL COMMENT '类别 1离职 2辞退',
    exit_type TINYINT NOT NULL COMMENT '类型 1主动离职 2合同到期 3退休 4辞退 5裁员 6开除',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    emp_no VARCHAR(32) COMMENT '工号（冗余，便于单据还原）',
    employee_name VARCHAR(64) NOT NULL COMMENT '员工姓名',
    dept_id BIGINT COMMENT '部门ID',
    dept_name VARCHAR(64) COMMENT '部门名称（冗余）',
    position VARCHAR(128) COMMENT '岗位（冗余）',
    apply_date DATE COMMENT '发起日期',
    expect_leave_date DATE NOT NULL COMMENT '预计离职/辞退生效日期',
    actual_leave_date DATE COMMENT '实际生效日期',
    reason VARCHAR(255) COMMENT '原因说明',
    handover_to VARCHAR(64) COMMENT '工作交接人',
    compensation DECIMAL(12, 2) DEFAULT 0 COMMENT '补偿金额（辞退/裁员适用）',
    status TINYINT DEFAULT 0 COMMENT '状态 0待审批 1已批准 2已驳回 3已生效 4已撤销',
    approve_by VARCHAR(64) COMMENT '审批人',
    approve_time DATETIME COMMENT '审批时间',
    approve_remark VARCHAR(255) COMMENT '审批意见',
    create_by VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_exit_no (exit_no),
    INDEX idx_exit_emp (employee_id),
    INDEX idx_exit_status (exit_kind, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '员工离职/辞退单据';

-- 演示数据：一条待审批辞退 + 一条已批准待生效离职（仅当表为空时插入，避免重复执行产生脏数据）
INSERT INTO hr_employee_exit
    (exit_no, exit_kind, exit_type, employee_id, emp_no, employee_name, dept_id, position,
     apply_date, expect_leave_date, reason, handover_to, compensation, status, create_by)
SELECT 'CT20261001', 2, 4, e.id, e.emp_no, e.name, e.dept_id, e.position,
       '2026-10-12', '2026-10-31', '岗位调整后绩效持续不达标，经沟通予以辞退', 'HR 主管', 12000.00, 0, 'admin'
FROM sys_employee e
WHERE e.deleted = 0 AND e.status <> 0
ORDER BY e.id DESC
LIMIT 1;

INSERT INTO hr_employee_exit
    (exit_no, exit_kind, exit_type, employee_id, emp_no, employee_name, dept_id, position,
     apply_date, expect_leave_date, reason, handover_to, status, create_by, approve_by, approve_time)
SELECT 'LZ20261001', 1, 1, e.id, e.emp_no, e.name, e.dept_id, e.position,
       '2026-10-08', '2026-10-31', '个人原因，主动提出离职', 'HR 主管', 1, 'admin', 'admin', '2026-10-09 10:20:00'
FROM sys_employee e
WHERE e.deleted = 0 AND e.status <> 0
ORDER BY e.id ASC
LIMIT 1;
