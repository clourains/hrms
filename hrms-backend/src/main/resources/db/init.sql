-- 创建数据库
CREATE DATABASE IF NOT EXISTS hrms DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hrms;

-- 部门表
CREATE TABLE IF NOT EXISTS sys_department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '部门ID',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    dept_code VARCHAR(50) NOT NULL COMMENT '部门编码',
    parent_id BIGINT DEFAULT 0 COMMENT '上级部门ID',
    leader VARCHAR(50) COMMENT '部门负责人',
    phone VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态 0停用 1正常',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    UNIQUE KEY uk_dept_code (dept_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 员工表
CREATE TABLE IF NOT EXISTS sys_employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '员工ID',
    emp_no VARCHAR(50) NOT NULL COMMENT '工号',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender TINYINT COMMENT '性别 0女 1男',
    birthday DATE COMMENT '出生日期',
    id_card VARCHAR(18) COMMENT '身份证号',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    address VARCHAR(255) COMMENT '住址',
    avatar VARCHAR(255) COMMENT '头像',
    dept_id BIGINT COMMENT '部门ID',
    position VARCHAR(50) COMMENT '职位',
    job_level VARCHAR(50) COMMENT '职级',
    entry_date DATE COMMENT '入职日期',
    work_years INT DEFAULT 0 COMMENT '工龄(年)',
    education VARCHAR(20) COMMENT '学历',
    major VARCHAR(50) COMMENT '专业',
    salary DECIMAL(10,2) DEFAULT 0 COMMENT '基础薪资',
    status TINYINT DEFAULT 1 COMMENT '状态 0离职 1在职 2试用期 3休假',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_dept_id (dept_id),
    INDEX idx_emp_no (emp_no),
    UNIQUE KEY uk_emp_no (emp_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

-- 用户表（登录用）
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像',
    role VARCHAR(20) DEFAULT 'user' COMMENT '角色 admin/user',
    emp_id BIGINT COMMENT '关联员工ID',
    status TINYINT DEFAULT 1 COMMENT '状态 0停用 1正常',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 考勤表
CREATE TABLE IF NOT EXISTS sys_attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '考勤ID',
    emp_id BIGINT NOT NULL COMMENT '员工ID',
    attend_date DATE NOT NULL COMMENT '考勤日期',
    check_in_time DATETIME COMMENT '上班打卡时间',
    check_out_time DATETIME COMMENT '下班打卡时间',
    status TINYINT DEFAULT 1 COMMENT '状态 1正常 2迟到 3早退 4缺勤 5请假 6出差',
    work_hours DECIMAL(4,2) DEFAULT 0 COMMENT '工作时长(小时)',
    remark VARCHAR(255) COMMENT '备注',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_emp_id (emp_id),
    INDEX idx_attend_date (attend_date),
    UNIQUE KEY uk_emp_date (emp_id, attend_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤表';

-- 薪资表
CREATE TABLE IF NOT EXISTS sys_salary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '薪资ID',
    emp_id BIGINT NOT NULL COMMENT '员工ID',
    salary_month VARCHAR(7) NOT NULL COMMENT '薪资月份 yyyy-MM',
    base_salary DECIMAL(10,2) DEFAULT 0 COMMENT '基本工资',
    performance_bonus DECIMAL(10,2) DEFAULT 0 COMMENT '绩效奖金',
    overtime_pay DECIMAL(10,2) DEFAULT 0 COMMENT '加班费',
    allowance DECIMAL(10,2) DEFAULT 0 COMMENT '补贴',
    social_security DECIMAL(10,2) DEFAULT 0 COMMENT '社保扣除',
    housing_fund DECIMAL(10,2) DEFAULT 0 COMMENT '公积金扣除',
    tax DECIMAL(10,2) DEFAULT 0 COMMENT '个税扣除',
    other_deduction DECIMAL(10,2) DEFAULT 0 COMMENT '其他扣除',
    net_salary DECIMAL(10,2) DEFAULT 0 COMMENT '实发工资',
    status TINYINT DEFAULT 0 COMMENT '状态 0未发放 1已发放',
    pay_time DATETIME COMMENT '发放时间',
    remark VARCHAR(255) COMMENT '备注',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_emp_id (emp_id),
    INDEX idx_salary_month (salary_month),
    -- P042：不用 UNIQUE KEY，逻辑删除的行会永久占用唯一键导致删后无法重建；
    -- 同员工同月的防重由 Service 层查重保证（仅对未删除记录）
    KEY idx_emp_month (emp_id, salary_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资表';

-- 绩效表
CREATE TABLE IF NOT EXISTS sys_performance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '绩效ID',
    emp_id BIGINT NOT NULL COMMENT '员工ID',
    period VARCHAR(7) NOT NULL COMMENT '考核周期 yyyy-MM',
    work_quality DECIMAL(5,2) DEFAULT 0 COMMENT '工作质量(0-100)',
    work_efficiency DECIMAL(5,2) DEFAULT 0 COMMENT '工作效率(0-100)',
    work_attitude DECIMAL(5,2) DEFAULT 0 COMMENT '工作态度(0-100)',
    teamwork DECIMAL(5,2) DEFAULT 0 COMMENT '团队协作(0-100)',
    innovation DECIMAL(5,2) DEFAULT 0 COMMENT '创新能力(0-100)',
    total_score DECIMAL(5,2) DEFAULT 0 COMMENT '综合得分',
    grade VARCHAR(10) COMMENT '等级 S/A/B/C/D',
    evaluator VARCHAR(50) COMMENT '考核人',
    comment VARCHAR(500) COMMENT '评语',
    status TINYINT DEFAULT 0 COMMENT '状态 0草稿 1已提交 2已确认',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_emp_id (emp_id),
    INDEX idx_period (period),
    -- 同上：逻辑删除与唯一键冲突，防重由 Service 层查重保证
    KEY idx_emp_period (emp_id, period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绩效表';

-- 招聘表
CREATE TABLE IF NOT EXISTS sys_recruitment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '招聘ID',
    position VARCHAR(100) NOT NULL COMMENT '招聘职位',
    dept_id BIGINT COMMENT '所属部门',
    headcount INT DEFAULT 1 COMMENT '招聘人数',
    salary_range VARCHAR(50) COMMENT '薪资范围',
    education VARCHAR(20) COMMENT '学历要求',
    experience VARCHAR(50) COMMENT '经验要求',
    description TEXT COMMENT '职位描述',
    requirement TEXT COMMENT '任职要求',
    status TINYINT DEFAULT 1 COMMENT '状态 0关闭 1招聘中 2已完成',
    publisher VARCHAR(50) COMMENT '发布人',
    publish_time DATETIME COMMENT '发布时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招聘表';

-- 培训表
CREATE TABLE IF NOT EXISTS sys_training (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '培训ID',
    title VARCHAR(200) NOT NULL COMMENT '培训标题',
    trainer VARCHAR(50) COMMENT '讲师',
    training_type VARCHAR(20) COMMENT '培训类型',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    location VARCHAR(100) COMMENT '培训地点',
    description TEXT COMMENT '培训内容',
    max_participants INT DEFAULT 50 COMMENT '最大人数',
    participants INT DEFAULT 0 COMMENT '已报名人数',
    status TINYINT DEFAULT 1 COMMENT '状态 0取消 1报名中 2进行中 3已结束',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培训表';

-- 员工培训关联表
CREATE TABLE IF NOT EXISTS sys_emp_training (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    emp_id BIGINT NOT NULL COMMENT '员工ID',
    training_id BIGINT NOT NULL COMMENT '培训ID',
    score DECIMAL(5,2) COMMENT '培训成绩',
    status TINYINT DEFAULT 1 COMMENT '状态 1报名 2完成 3未通过',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_emp_id (emp_id),
    INDEX idx_training_id (training_id),
    UNIQUE KEY uk_emp_training (emp_id, training_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工培训关联表';

-- 初始化数据
-- 部门数据
INSERT IGNORE INTO sys_department (dept_name, dept_code, parent_id, leader, phone, email, sort) VALUES
('总公司', 'HQ', 0, '张总', '13800000001', 'ceo@company.com', 1),
('技术部', 'TECH', 1, '李技术', '13800000002', 'tech@company.com', 2),
('市场部', 'MARKET', 1, '王市场', '13800000003', 'market@company.com', 3),
('人事部', 'HR', 1, '赵人事', '13800000004', 'hr@company.com', 4),
('财务部', 'FINANCE', 1, '钱财务', '13800000005', 'finance@company.com', 5),
('行政部', 'ADMIN', 1, '孙行政', '13800000006', 'admin@company.com', 6),
('研发一组', 'TECH-1', 2, '周组长', '13800000007', 'tech1@company.com', 1),
('研发二组', 'TECH-2', 2, '吴组长', '13800000008', 'tech2@company.com', 2),
('运维组', 'TECH-3', 2, '郑组长', '13800000009', 'tech3@company.com', 3);

-- 员工数据
INSERT IGNORE INTO sys_employee (emp_no, name, gender, birthday, id_card, phone, email, address, dept_id, position, job_level, entry_date, work_years, education, major, salary, status) VALUES
('EMP001', '张总', 1, '1980-01-15', '110101198001150001', '13800000001', 'ceo@company.com', '北京市朝阳区', 1, '总经理', 'P10', '2010-01-01', 14, '博士', '工商管理', 50000.00, 1),
('EMP002', '李技术', 1, '1985-03-20', '110101198503200002', '13800000002', 'tech@company.com', '北京市海淀区', 2, '技术总监', 'P9', '2012-03-15', 12, '硕士', '计算机科学', 40000.00, 1),
('EMP003', '王市场', 0, '1986-05-10', '110101198605100003', '13800000003', 'market@company.com', '北京市西城区', 3, '市场总监', 'P9', '2013-06-01', 11, '硕士', '市场营销', 38000.00, 1),
('EMP004', '赵人事', 0, '1988-08-25', '110101198808250004', '13800000004', 'hr@company.com', '北京市东城区', 4, '人事总监', 'P8', '2014-02-10', 10, '本科', '人力资源', 32000.00, 1),
('EMP005', '钱财务', 0, '1987-11-30', '110101198711300005', '13800000005', 'finance@company.com', '北京市丰台区', 5, '财务总监', 'P8', '2013-09-20', 11, '本科', '会计学', 35000.00, 1),
('EMP006', '孙行政', 1, '1990-02-14', '110101199002140006', '13800000006', 'admin@company.com', '北京市石景山区', 6, '行政主管', 'P7', '2015-04-01', 9, '本科', '行政管理', 25000.00, 1),
('EMP007', '周组长', 1, '1990-07-08', '110101199007080007', '13800000007', 'tech1@company.com', '北京市通州区', 7, '研发组长', 'P7', '2015-08-15', 9, '硕士', '软件工程', 28000.00, 1),
('EMP008', '吴组长', 0, '1991-04-22', '110101199104220008', '13800000008', 'tech2@company.com', '北京市大兴区', 8, '研发组长', 'P7', '2016-01-10', 8, '本科', '计算机科学', 26000.00, 1),
('EMP009', '郑组长', 1, '1989-12-05', '110101198912050009', '13800000009', 'tech3@company.com', '北京市昌平区', 9, '运维组长', 'P6', '2014-11-20', 10, '本科', '网络工程', 24000.00, 1),
('EMP010', '陈开发', 1, '1993-06-18', '110101199306180010', '13800000010', 'chen@company.com', '北京市朝阳区', 7, '高级开发工程师', 'P6', '2017-03-01', 7, '本科', '软件工程', 22000.00, 1),
('EMP011', '林开发', 0, '1994-09-25', '110101199409250011', '13800000011', 'lin@company.com', '北京市海淀区', 7, '中级开发工程师', 'P5', '2018-05-10', 6, '本科', '计算机科学', 18000.00, 1),
('EMP012', '黄开发', 1, '1995-11-12', '110101199511120012', '13800000012', 'huang@company.com', '北京市西城区', 8, '高级开发工程师', 'P6', '2017-07-20', 7, '硕士', '软件工程', 23000.00, 1),
('EMP013', '徐测试', 0, '1996-03-08', '110101199603080013', '13800000013', 'xu@company.com', '北京市东城区', 8, '测试工程师', 'P5', '2018-09-01', 6, '本科', '软件工程', 16000.00, 1),
('EMP014', '朱运维', 1, '1994-08-15', '110101199408150014', '13800000014', 'zhu@company.com', '北京市丰台区', 9, '运维工程师', 'P5', '2017-11-10', 7, '本科', '网络工程', 17000.00, 1),
('EMP015', '马销售', 1, '1992-01-20', '110101199201200015', '13800000015', 'ma@company.com', '北京市朝阳区', 3, '销售经理', 'P6', '2016-04-15', 8, '本科', '市场营销', 20000.00, 1),
('EMP016', '何销售', 0, '1995-07-30', '110101199507300016', '13800000016', 'he@company.com', '北京市海淀区', 3, '销售专员', 'P4', '2019-02-20', 5, '本科', '市场营销', 12000.00, 1),
('EMP017', '高招聘', 0, '1993-12-10', '110101199312100017', '13800000017', 'gao@company.com', '北京市西城区', 4, '招聘专员', 'P5', '2018-06-01', 6, '本科', '人力资源', 14000.00, 1),
('EMP018', '梁薪酬', 1, '1991-05-25', '110101199105250018', '13800000018', 'liang@company.com', '北京市东城区', 4, '薪酬专员', 'P5', '2016-08-10', 8, '本科', '人力资源', 15000.00, 1),
('EMP019', '宋会计', 0, '1990-10-05', '110101199010050019', '13800000019', 'song@company.com', '北京市丰台区', 5, '会计', 'P5', '2015-12-01', 9, '本科', '会计学', 16000.00, 1),
('EMP020', '唐出纳', 0, '1996-02-28', '110101199602280020', '13800000020', 'tang@company.com', '北京市石景山区', 5, '出纳', 'P4', '2019-08-15', 5, '本科', '金融学', 11000.00, 1);

-- 用户数据 密码都是123456 (MD5加密)
-- 种子账号初始密码均为 123456（MD5 仅作首次引导，P046）：
-- 登录校验为 BCrypt 优先 + MD5 兼容回退，存量账号首次登录成功后密码自动升级为 BCrypt 加盐哈希
INSERT IGNORE INTO sys_user (username, password, nickname, role, emp_id, status) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 'admin', 1, 1),
('hr001', 'e10adc3949ba59abbe56e057f20f883e', '赵人事', 'admin', 4, 1),
('emp001', 'e10adc3949ba59abbe56e057f20f883e', '张总', 'user', 1, 1),
('emp002', 'e10adc3949ba59abbe56e057f20f883e', '李技术', 'user', 2, 1),
('emp003', 'e10adc3949ba59abbe56e057f20f883e', '陈开发', 'user', 10, 1);

-- 考勤数据（最近30天）
INSERT IGNORE INTO sys_attendance (emp_id, attend_date, check_in_time, check_out_time, status, work_hours, remark)
SELECT 
    e.id,
    DATE_SUB(CURDATE(), INTERVAL (t.n - 1) DAY),
    IF(DAYOFWEEK(DATE_SUB(CURDATE(), INTERVAL (t.n - 1) DAY)) NOT IN (1,7), 
       CONCAT(DATE_SUB(CURDATE(), INTERVAL (t.n - 1) DAY), ' ', 
              IF(RAND() > 0.1, '09:', '09:') + FLOOR(RAND() * 30), ':', FLOOR(RAND() * 60)),
       NULL),
    IF(DAYOFWEEK(DATE_SUB(CURDATE(), INTERVAL (t.n - 1) DAY)) NOT IN (1,7),
       CONCAT(DATE_SUB(CURDATE(), INTERVAL (t.n - 1) DAY), ' ', 
              IF(RAND() > 0.8, '18:', '18:') + FLOOR(RAND() * 60), ':', FLOOR(RAND() * 60)),
       NULL),
    CASE 
        WHEN DAYOFWEEK(DATE_SUB(CURDATE(), INTERVAL (t.n - 1) DAY)) IN (1,7) THEN 4
        WHEN RAND() < 0.05 THEN 2
        WHEN RAND() < 0.05 THEN 3
        WHEN RAND() < 0.02 THEN 5
        ELSE 1
    END,
    IF(DAYOFWEEK(DATE_SUB(CURDATE(), INTERVAL (t.n - 1) DAY)) NOT IN (1,7), 8 + RAND() * 2, 0),
    ''
FROM sys_employee e
CROSS JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
    UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
    UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25
    UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
) t
WHERE e.id <= 20;

-- 薪资数据（最近6个月）
INSERT IGNORE INTO sys_salary (emp_id, salary_month, base_salary, performance_bonus, overtime_pay, allowance, social_security, housing_fund, tax, other_deduction, net_salary, status, pay_time)
SELECT 
    e.id,
    DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (t.n) MONTH), '%Y-%m'),
    e.salary,
    ROUND(e.salary * (0.1 + RAND() * 0.2), 2),
    ROUND(RAND() * 1000, 2),
    500,
    ROUND(e.salary * 0.105, 2),
    ROUND(e.salary * 0.12, 2),
    ROUND((e.salary - 5000) * 0.1, 2),
    0,
    ROUND(e.salary * 1.15 - e.salary * 0.225 - (e.salary - 5000) * 0.1 + 500, 2),
    1,
    DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (t.n) MONTH), INTERVAL 10 DAY)
FROM sys_employee e
CROSS JOIN (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
) t
WHERE e.id <= 20;

-- 绩效数据（最近6个月）
INSERT IGNORE INTO sys_performance (emp_id, period, work_quality, work_efficiency, work_attitude, teamwork, innovation, total_score, grade, evaluator, comment, status)
SELECT 
    e.id,
    DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (t.n) MONTH), '%Y-%m'),
    ROUND(70 + RAND() * 30, 2),
    ROUND(70 + RAND() * 30, 2),
    ROUND(75 + RAND() * 25, 2),
    ROUND(70 + RAND() * 30, 2),
    ROUND(60 + RAND() * 35, 2),
    0,
    '',
    '系统评估',
    '本月工作表现',
    2
FROM sys_employee e
CROSS JOIN (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
) t
WHERE e.id <= 20;

-- 更新绩效总分和等级
UPDATE sys_performance SET 
    total_score = ROUND((work_quality * 0.25 + work_efficiency * 0.25 + work_attitude * 0.2 + teamwork * 0.15 + innovation * 0.15), 2),
    grade = CASE 
        WHEN (work_quality * 0.25 + work_efficiency * 0.25 + work_attitude * 0.2 + teamwork * 0.15 + innovation * 0.15) >= 90 THEN 'S'
        WHEN (work_quality * 0.25 + work_efficiency * 0.25 + work_attitude * 0.2 + teamwork * 0.15 + innovation * 0.15) >= 80 THEN 'A'
        WHEN (work_quality * 0.25 + work_efficiency * 0.25 + work_attitude * 0.2 + teamwork * 0.15 + innovation * 0.15) >= 70 THEN 'B'
        WHEN (work_quality * 0.25 + work_efficiency * 0.25 + work_attitude * 0.2 + teamwork * 0.15 + innovation * 0.15) >= 60 THEN 'C'
        ELSE 'D'
    END;
