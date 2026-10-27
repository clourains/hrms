-- 绩效/薪资配置：考核周期、绩效指标、薪资结构

CREATE TABLE IF NOT EXISTS sys_assessment_cycle (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    cycle_name VARCHAR(64) NOT NULL COMMENT '周期名称',
    period VARCHAR(7) NOT NULL COMMENT '考核月份 yyyy-MM',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1进行中 0已结束',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cycle_period (period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考核周期';

CREATE TABLE IF NOT EXISTS sys_performance_indicator (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    indicator_name VARCHAR(64) NOT NULL COMMENT '指标名称',
    weight INT NOT NULL DEFAULT 0 COMMENT '权重百分比',
    description VARCHAR(255) DEFAULT NULL COMMENT '指标说明',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绩效指标配置';

CREATE TABLE IF NOT EXISTS sys_salary_structure (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    item_name VARCHAR(64) NOT NULL COMMENT '项目名称',
    item_type VARCHAR(16) NOT NULL DEFAULT 'base' COMMENT 'base底薪/bonus奖金/deduction扣款',
    calc_type VARCHAR(16) NOT NULL DEFAULT 'percent' COMMENT 'percent按比例/fixed固定值',
    item_value DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '比例(%)或金额',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资结构配置';

-- 默认考核周期（幂等）
INSERT INTO sys_assessment_cycle (cycle_name, period, start_date, end_date, status)
SELECT '2026年10月考核', '2026-10', '2026-10-01', '2026-10-31', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_assessment_cycle WHERE period = '2026-10');

-- 默认绩效指标（幂等）
INSERT INTO sys_performance_indicator (indicator_name, weight, description, sort)
SELECT * FROM (
    SELECT '工作业绩' AS indicator_name, 40 AS weight, '工作目标完成情况、工作质量和效率' AS description, 1 AS sort
    UNION ALL SELECT '工作能力', 25, '专业技能、学习能力、问题解决能力', 2
    UNION ALL SELECT '工作态度', 20, '责任心、积极性、团队协作', 3
    UNION ALL SELECT '创新改进', 15, '创新意识、流程优化、知识分享', 4
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM sys_performance_indicator);

-- 默认薪资结构（幂等）
INSERT INTO sys_salary_structure (item_name, item_type, calc_type, item_value, sort)
SELECT * FROM (
    SELECT '基本工资' AS item_name, 'base' AS item_type, 'fixed' AS calc_type, 0 AS item_value, 1 AS sort
    UNION ALL SELECT '绩效奖金', 'bonus', 'percent', 20, 2
    UNION ALL SELECT '社保个人部分', 'deduction', 'percent', 10.5, 3
    UNION ALL SELECT '公积金个人部分', 'deduction', 'percent', 7, 4
    UNION ALL SELECT '个人所得税', 'deduction', 'percent', 3, 5
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM sys_salary_structure);
