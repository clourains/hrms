-- P5-3 系统参数配置：薪资引擎等硬编码参数后台可配
CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL COMMENT '参数键',
    config_value VARCHAR(500) NOT NULL COMMENT '参数值',
    remark VARCHAR(255) DEFAULT NULL COMMENT '参数说明',
    update_by VARCHAR(50) DEFAULT NULL COMMENT '最后修改人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数配置';

-- 薪资引擎参数（与 SalaryServiceImpl 原硬编码默认值保持一致）
INSERT IGNORE INTO sys_config (config_key, config_value, remark) VALUES
('salary.socialSecurityRate', '0.105', '社保个人缴纳比例（占底薪）'),
('salary.housingFundRate', '0.07', '公积金个人缴纳比例（占底薪）'),
('salary.lateEarlyFine', '50', '迟到/早退单次扣款（元）'),
('salary.performanceMaxRate', '0.2', '绩效奖金上限系数（满分绩效可拿底薪的比例）'),
('salary.performanceFullScore', '500', '绩效满分（5个维度×100分）'),
('salary.workDaysBase', '21.75', '月计薪天数基数'),
('salary.taxThreshold', '5000', '个税起征点（月，元）'),
('attendance.otWeekdayRate', '1.5', '工作日加班时薪倍率'),
('attendance.otWeekendRate', '2.0', '周末加班时薪倍率');
