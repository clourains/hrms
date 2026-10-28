-- 批量导入审计日志：Excel 上传与开放接口写入都留痕，便于追溯「谁、什么时候、导入了多少」
-- 只记录数量与来源，不记录具体业务字段值，避免把客户联系方式等敏感信息落到日志表
CREATE TABLE IF NOT EXISTS `sys_import_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `module` varchar(30) NOT NULL COMMENT '模块标识',
    `module_label` varchar(40) DEFAULT NULL COMMENT '模块中文名',
    `source` varchar(10) NOT NULL COMMENT '来源 EXCEL/API',
    `operator` varchar(60) DEFAULT NULL COMMENT '操作人（API 调用记为 open-api）',
    `ip` varchar(64) DEFAULT NULL COMMENT '来源IP',
    `file_name` varchar(200) DEFAULT NULL COMMENT '上传文件名（API 写入为空）',
    `total` int DEFAULT '0' COMMENT '总行数',
    `success` int DEFAULT '0' COMMENT '成功条数',
    `skipped` int DEFAULT '0' COMMENT '跳过条数',
    `failed` int DEFAULT '0' COMMENT '失败条数',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
    PRIMARY KEY (`id`),
    KEY `idx_import_module_time` (`module`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量导入审计日志';
