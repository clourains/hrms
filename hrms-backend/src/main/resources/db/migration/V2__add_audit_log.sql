-- P1-1 审计日志：敏感操作留痕（谁、何时、对哪个模块做了什么、结果如何）
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '操作人账号',
    module VARCHAR(50) NOT NULL COMMENT '业务模块，如：薪资管理',
    action VARCHAR(50) NOT NULL COMMENT '操作类型，如：新增/修改/删除',
    detail VARCHAR(1000) DEFAULT NULL COMMENT '参数摘要',
    success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功：1成功 0失败',
    error_msg VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    cost_ms BIGINT DEFAULT NULL COMMENT '耗时（毫秒）',
    ip VARCHAR(50) DEFAULT NULL COMMENT '操作IP',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_audit_time (create_time),
    KEY idx_audit_user (username),
    KEY idx_audit_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感操作审计日志';
