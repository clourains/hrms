-- P1-2 到期提醒：员工合同台账（签证信息沿用 sys_employee 已有字段 passport_no/visa_type/visa_expire_date）
CREATE TABLE IF NOT EXISTS sys_contract (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    emp_id BIGINT NOT NULL COMMENT '员工ID',
    contract_no VARCHAR(50) DEFAULT NULL COMMENT '合同编号',
    contract_type VARCHAR(30) DEFAULT '劳动合同' COMMENT '合同类型（劳动合同/劳务协议/保密协议等）',
    start_date DATE NOT NULL COMMENT '合同开始日期',
    end_date DATE NOT NULL COMMENT '合同结束日期',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1有效 0已作废',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_contract_emp (emp_id),
    KEY idx_contract_end (end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工合同台账';
