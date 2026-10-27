-- =============================================================
-- V2 招聘管理模块（招聘需求 -> 候选人 -> 面试 -> 试用期转正）
--
-- 设计原则：
-- 1. 不改动 sys_employee 等既有表，试用期起止存在 hr_recruit_probation 里，
--    员工表只被"录用"和"转正"动作更新 status，避免与他人改动产生结构冲突。
-- 2. 表名统一 hr_recruit_ 前缀，与 sys_ 系统表、biz_ 贸易业务表区分开。
-- 3. 全部 CREATE TABLE IF NOT EXISTS + 不用外键约束（与项目现有风格一致，
--    删除靠逻辑删除和 Service 层引用检查），可重复执行不报错。
-- 4. 编码列强制唯一索引，逻辑删除时由 CodeReleaser 释放编码，方案与
--   biz_customer / biz_supplier 保持一致。
-- =============================================================

-- -------------------------------------------------------------
-- 招聘需求
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_recruit_requisition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '需求ID',
    req_no VARCHAR(32) NOT NULL COMMENT '需求编号，如 REQ20261026001',
    position VARCHAR(64) NOT NULL COMMENT '招聘岗位',
    dept_id BIGINT NOT NULL COMMENT '需求部门ID',
    headcount INT NOT NULL DEFAULT 1 COMMENT '计划招聘人数',
    hired_count INT NOT NULL DEFAULT 0 COMMENT '已录用人数（录用候选人时累加）',
    urgency TINYINT DEFAULT 1 COMMENT '紧急程度 1一般 2紧急 3非常紧急',
    salary_min DECIMAL(10,2) COMMENT '薪资下限（K/月）',
    salary_max DECIMAL(10,2) COMMENT '薪资上限（K/月）',
    education VARCHAR(20) COMMENT '学历要求',
    experience VARCHAR(20) COMMENT '经验要求',
    work_location VARCHAR(64) COMMENT '工作地点',
    job_duty TEXT COMMENT '岗位职责',
    requirement TEXT COMMENT '任职要求',
    publish_date DATE COMMENT '发布日期',
    deadline DATE COMMENT '期望到岗日期',
    applicant VARCHAR(50) COMMENT '需求提出人',
    status TINYINT DEFAULT 1 COMMENT '状态 1招聘中 2已暂停 3已完成 4已关闭',
    remark VARCHAR(255) COMMENT '备注',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_req_no (req_no),
    INDEX idx_req_dept_id (dept_id),
    INDEX idx_req_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招聘需求表';

-- -------------------------------------------------------------
-- 候选人
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_recruit_candidate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '候选人ID',
    candidate_no VARCHAR(32) NOT NULL COMMENT '候选人编号，如 C20261026001',
    requisition_id BIGINT NOT NULL COMMENT '应聘的招聘需求ID',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender TINYINT DEFAULT 1 COMMENT '性别 0女 1男',
    birthday DATE COMMENT '出生日期',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    education VARCHAR(20) COMMENT '学历',
    school VARCHAR(100) COMMENT '毕业院校',
    major VARCHAR(50) COMMENT '专业',
    work_years INT DEFAULT 0 COMMENT '工作年限',
    current_company VARCHAR(100) COMMENT '当前/最近公司',
    current_position VARCHAR(50) COMMENT '当前/最近职位',
    expect_salary DECIMAL(10,2) COMMENT '期望薪资（K/月）',
    source VARCHAR(30) COMMENT '来源：简历投递/内部推荐/猎头/招聘网站/校园招聘',
    stage TINYINT DEFAULT 1 COMMENT '阶段 1待筛选 2初试 3复试 4待录用 5已录用 6已淘汰',
    employee_id BIGINT COMMENT '录用后生成的员工ID',
    reject_reason VARCHAR(255) COMMENT '淘汰原因',
    remark VARCHAR(255) COMMENT '备注',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_candidate_no (candidate_no),
    INDEX idx_cand_requisition_id (requisition_id),
    INDEX idx_cand_stage (stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招聘候选人表';

-- -------------------------------------------------------------
-- 面试安排
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_recruit_interview (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '面试ID',
    candidate_id BIGINT NOT NULL COMMENT '候选人ID',
    requisition_id BIGINT COMMENT '招聘需求ID（冗余，列表按需求筛选用）',
    round TINYINT DEFAULT 1 COMMENT '轮次 1初试 2复试 3终试',
    interview_type TINYINT DEFAULT 1 COMMENT '形式 1现场 2电话 3视频',
    interview_time DATETIME NOT NULL COMMENT '面试时间',
    duration INT DEFAULT 60 COMMENT '时长（分钟）',
    interviewer VARCHAR(100) COMMENT '面试官，多人用逗号分隔',
    location VARCHAR(100) COMMENT '面试地点/会议链接',
    result TINYINT DEFAULT 0 COMMENT '结果 0待面试 1通过 2未通过 3候选人爽约',
    score INT COMMENT '面试评分 0-100',
    comment VARCHAR(500) COMMENT '面试评价',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_int_candidate_id (candidate_id),
    INDEX idx_int_time (interview_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试安排表';

-- -------------------------------------------------------------
-- 试用期 / 转正
--
-- 一条员工只应有一条进行中的试用记录，唯一性由 Service 层校验：
-- 表上不加唯一键，否则逻辑删除的历史记录会把该员工永久占住。
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_recruit_probation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '试用期ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    candidate_id BIGINT COMMENT '来源候选人ID，手工登记时为空',
    dept_id BIGINT COMMENT '所属部门ID',
    position VARCHAR(50) COMMENT '职位',
    probation_start DATE NOT NULL COMMENT '试用期开始日期（通常为入职日）',
    probation_end DATE NOT NULL COMMENT '试用期结束日期',
    mentor VARCHAR(50) COMMENT '带教导师',
    status TINYINT DEFAULT 1 COMMENT '状态 1试用中 2已转正 3未通过 4已延期',
    confirm_date DATE COMMENT '实际转正/终止日期',
    score INT COMMENT '转正评分 0-100',
    evaluation VARCHAR(500) COMMENT '转正评价',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_prob_employee_id (employee_id),
    INDEX idx_prob_end (probation_end),
    INDEX idx_prob_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试用期转正表';
