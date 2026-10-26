-- P3-1 多级审批流：通用审批实例 + 逐级节点记录
CREATE TABLE IF NOT EXISTS sys_approval_instance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz_type VARCHAR(30) NOT NULL DEFAULT '通用' COMMENT '业务类型：请假/调薪/通用',
    title VARCHAR(200) NOT NULL COMMENT '申请标题',
    detail TEXT COMMENT '申请内容 JSON（请假：startDate/endDate/days/reason）',
    applicant VARCHAR(50) NOT NULL COMMENT '申请人用户名',
    applicant_name VARCHAR(50) DEFAULT NULL COMMENT '申请人姓名冗余',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0审批中 1已通过 2已驳回 3已撤销',
    current_node INT NOT NULL DEFAULT 1 COMMENT '当前节点序号（1起）',
    node_count INT NOT NULL DEFAULT 1 COMMENT '审批级数',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_appr_applicant (applicant),
    KEY idx_appr_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批实例';

CREATE TABLE IF NOT EXISTS sys_approval_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instance_id BIGINT NOT NULL COMMENT '审批实例ID',
    node_order INT NOT NULL COMMENT '节点序号（1起）',
    approver VARCHAR(50) NOT NULL COMMENT '审批人用户名',
    approver_name VARCHAR(50) DEFAULT NULL COMMENT '审批人姓名冗余',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2驳回',
    comment VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    approve_time DATETIME DEFAULT NULL COMMENT '处理时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_rec_instance (instance_id),
    KEY idx_rec_approver (approver, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批节点记录';
