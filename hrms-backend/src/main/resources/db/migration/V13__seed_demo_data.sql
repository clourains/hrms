-- V13 演示数据补齐
--
-- 背景：V1__baseline.sql 是一份 2026-10-15 的数据快照，只覆盖了部门/员工/用户/考勤/薪资/
-- 绩效和外贸主数据。后加的模块（合同 V3、站内信 V4、审批 V5、加班+额度 V6、公告 V9、
-- 招聘四张表 V10）只建了表、没有数据，导致全新装好的系统里有十几个页面一打开就是空表格，
-- 用户分不清是「没数据」还是「功能坏了」。
--
-- 本脚本补齐这些空表，让每个页面都有内容可看、可演示、可日常使用。
-- 日期统一用 CURDATE() 相对取，保证任何时候安装都能看到「临期」效果，不会随时间失效。
--
-- 幂等性：每张表都做了「已存在则跳过」的判断，重复执行不会产生重复数据。
--
-- 注意：派生表的第一行列必须起别名，否则相邻相同字面量会被当成重复列名（ERROR 1060）。

-- ============================================================================
-- 1. 劳动合同台账（sys_contract）
--    到期提醒页的「合同到期」从此有数据；刻意安排 1 条已过期、3 条 30 天内到期
-- ============================================================================
INSERT INTO sys_contract (emp_id, contract_no, contract_type, start_date, end_date, status, remark)
SELECT e.id,
       CONCAT('HT-', e.emp_no, '-', YEAR(CURDATE())),
       '劳动合同',
       DATE_SUB(DATE_ADD(CURDATE(), INTERVAL d.days DAY), INTERVAL 3 YEAR),
       DATE_ADD(CURDATE(), INTERVAL d.days DAY),
       1,
       CASE
           WHEN d.days < 0 THEN '合同已过期，请尽快办理续签'
           WHEN d.days <= 30 THEN '即将到期，请安排续签'
           ELSE '正常履行中'
       END
FROM sys_employee e
JOIN (
    SELECT 1 AS emp_id, 25 AS days UNION ALL SELECT 2, 45  UNION ALL SELECT 3, 8
    UNION ALL SELECT 4, -12        UNION ALL SELECT 5, 150 UNION ALL SELECT 6, 400
    UNION ALL SELECT 7, 700        UNION ALL SELECT 8, 90  UNION ALL SELECT 9, 520
    UNION ALL SELECT 10, 60        UNION ALL SELECT 11, 300 UNION ALL SELECT 12, 850
    UNION ALL SELECT 13, 180       UNION ALL SELECT 14, 640 UNION ALL SELECT 15, 15
    UNION ALL SELECT 16, 1000      UNION ALL SELECT 17, 260 UNION ALL SELECT 18, 730
) d ON d.emp_id = e.id
LEFT JOIN sys_contract c ON c.emp_id = e.id AND c.deleted = 0
WHERE c.id IS NULL;

-- ============================================================================
-- 2. 招聘模块：需求 → 候选人 → 面试 → 试用期，一条完整链路
-- ============================================================================
INSERT INTO hr_recruit_requisition
    (req_no, position, dept_id, headcount, hired_count, urgency, salary_min, salary_max,
     education, experience, work_location, job_duty, requirement, publish_date, deadline,
     applicant, status, remark)
SELECT c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17, c18
FROM (
    SELECT 'REQ202609-001' AS c1, '外贸业务员' AS c2, 3 AS c3, 3 AS c4, 1 AS c5, 2 AS c6,
           8.00 AS c7, 15.00 AS c8, '本科' AS c9, '1-3年' AS c10, '深圳' AS c11,
           '负责海外客户开发与维护，跟进订单全流程' AS c12,
           '英语 CET-6，有外贸经验优先' AS c13,
           DATE_SUB(CURDATE(), INTERVAL 40 DAY) AS c14,
           DATE_ADD(CURDATE(), INTERVAL 20 DAY) AS c15, 'admin' AS c16, 1 AS c17, '重点岗位' AS c18
    UNION ALL SELECT 'REQ202609-002', 'Java 开发工程师', 2, 2, 0, 3, 15.00, 25.00, '本科', '3-5年', '深圳',
           '参与 HRMS 系统后端开发与维护', '熟悉 Spring Boot / MyBatis，有 SaaS 经验优先',
           DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY), 'admin', 1, '急招'
    UNION ALL SELECT 'REQ202610-001', '海外仓运营专员', 2, 2, 0, 1, 7.00, 12.00, '大专', '1年以上', '洛杉矶',
           '负责美国海外仓日常运营与库存管理', '有海外仓或跨境物流经验优先',
           DATE_SUB(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 45 DAY), 'admin', 1, NULL
    UNION ALL SELECT 'REQ202608-001', '薪酬绩效专员', 4, 1, 1, 1, 9.00, 14.00, '本科', '2年以上', '深圳',
           '负责薪酬核算与绩效体系落地', '熟悉劳动法与薪酬设计',
           DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY), 'admin', 3, '已完成'
) t
WHERE NOT EXISTS (SELECT 1 FROM hr_recruit_requisition r WHERE r.req_no = 'REQ202609-001');

-- 候选人：需求1挂3人（1人已录用）、需求2挂3人、需求3挂2人、需求4挂1人（已录用）
INSERT INTO hr_recruit_candidate
    (candidate_no, requisition_id, name, gender, birthday, phone, email, education, school,
     major, work_years, current_company, current_position, expect_salary, source, stage, remark)
SELECT c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17
FROM (
    SELECT 'C202609-001' AS c1, 1 AS c2, '刘思远' AS c3, 1 AS c4, '1996-04-12' AS c5,
           '13900000001' AS c6, 'liu@example.com' AS c7, '本科' AS c8, '广东外语外贸大学' AS c9,
           '国际经济与贸易' AS c10, 3 AS c11, '某外贸公司' AS c12, '外贸业务员' AS c13,
           12.00 AS c14, '招聘网站' AS c15, 5 AS c16, '已录用，2026-09 入职' AS c17
    UNION ALL SELECT 'C202609-002', 1, '陈佳怡', 0, '1998-07-25', '13900000002', 'chenjy@example.com', '本科',
           '深圳大学', '商务英语', 2, '某跨境电商', '销售代表', 10.50, '简历投递', 4, '待发 offer'
    UNION ALL SELECT 'C202609-003', 1, '杨帆', 1, '1995-11-08', '13900000003', 'yangf@example.com', '大专',
           '深圳职业技术学院', '物流管理', 5, '某货代公司', '操作主管', 11.00, '内部推荐', 2, '初试已安排'
    UNION ALL SELECT 'C202609-004', 2, '赵鹏', 1, '1994-02-18', '13900000004', 'zhaop@example.com', '本科',
           '华中科技大学', '软件工程', 5, '某互联网公司', '后端工程师', 22.00, '猎头', 3, '复试通过，待终面'
    UNION ALL SELECT 'C202609-005', 2, '孙倩', 0, '1997-09-30', '13900000005', 'sunq@example.com', '硕士',
           '中山大学', '计算机技术', 2, '某软件公司', '研发工程师', 19.00, '招聘网站', 1, '待筛选'
    UNION ALL SELECT 'C202609-006', 2, '周浩', 1, '1993-05-14', '13900000006', 'zhouh@example.com', '本科',
           '电子科技大学', '计算机科学与技术', 8, '某外企', '高级工程师', 25.00, '猎头', 6, '薪资预期过高，已淘汰'
    UNION ALL SELECT 'C202610-001', 3, 'Kevin Brown', 1, '1992-03-22', '13900000007', 'kevin@example.com', '本科',
           'UCLA', 'Supply Chain', 6, 'US Logistics Inc', 'Warehouse Supervisor', 11.00, '招聘网站', 2, NULL
    UNION ALL SELECT 'C202610-002', 3, 'Emily Davis', 0, '1996-12-05', '13900000008', 'emily@example.com', '本科',
           'CSU Long Beach', 'Business Admin', 3, 'Global Trade Co', 'Operations Specialist', 9.50, '校园招聘', 1, NULL
    UNION ALL SELECT 'C202608-001', 4, '吴敏', 0, '1993-08-16', '13900000009', 'wum@example.com', '本科',
           '中南财经政法大学', '人力资源管理', 6, '某制造企业', '薪酬主管', 13.00, '内部推荐', 5, '已录用'
) t
WHERE NOT EXISTS (SELECT 1 FROM hr_recruit_candidate c WHERE c.candidate_no = 'C202609-001');

-- 面试安排
INSERT INTO hr_recruit_interview
    (candidate_id, requisition_id, `round`, interview_type, interview_time, duration,
     interviewer, location, result, score, comment)
SELECT c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11
FROM (
    SELECT 1 AS c1, 1 AS c2, 1 AS c3, 1 AS c4, '2026-09-12 10:00:00' AS c5, 60 AS c6,
           'admin,马销售' AS c7, '深圳总部 3 号会议室' AS c8, 1 AS c9, 85 AS c10,
           '沟通能力强，外贸流程熟悉' AS c11
    UNION ALL SELECT 1, 1, 2, 1, '2026-09-18 14:00:00', 60, 'admin', '深圳总部 2 号会议室', 1, 88, '业务理解到位，当场定薪'
    UNION ALL SELECT 2, 1, 1, 3, '2026-10-20 10:30:00', 45, '马销售', '腾讯会议 888-123-456', 1, 80, '英语口语流利'
    UNION ALL SELECT 3, 1, 1, 1, '2026-10-24 15:00:00', 60, '马销售', '深圳总部 3 号会议室', 0, NULL, NULL
    UNION ALL SELECT 4, 2, 1, 1, '2026-10-10 09:30:00', 90, 'admin,李技术', '深圳总部 1 号会议室', 1, 90, '技术基础扎实'
    UNION ALL SELECT 4, 2, 2, 2, '2026-10-16 15:00:00', 60, '李技术', '电话面试', 1, 86, '项目经验匹配'
    UNION ALL SELECT 6, 2, 1, 1, '2026-09-28 10:00:00', 60, '李技术', '深圳总部 1 号会议室', 2, 65, '技术符合要求但薪资超预算'
    UNION ALL SELECT 7, 3, 1, 3, '2026-10-26 20:00:00', 45, 'admin', 'Zoom 会议', 0, NULL, NULL
) t
WHERE NOT EXISTS (SELECT 1 FROM hr_recruit_interview i WHERE i.candidate_id = 1);

-- 试用期：21/22 已转正（历史），24 试用中且临期（触发侧边栏角标与转正页提醒）
-- 注意：employee_id 必须指向 deleted=0 的真实员工；原稿写死的 25 号在既有库里是已
-- 逻辑删除的测试员工，会导致试用期页姓名空白、转正操作找不到档案。这里改挂 24 号。
INSERT INTO hr_recruit_probation
    (employee_id, candidate_id, dept_id, position, probation_start, probation_end, mentor,
     status, confirm_date, score, evaluation)
SELECT c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11
FROM (
    SELECT 21 AS c1, NULL AS c2, 2 AS c3, '海外技术支持' AS c4, '2023-03-15' AS c5,
           '2023-06-15' AS c6, '李技术' AS c7, 2 AS c8, '2023-06-15' AS c9, 86 AS c10,
           '适应快，独立承担客户支持工作' AS c11
    UNION ALL SELECT 22, NULL, 2, '海外市场专员', '2023-06-01', '2023-09-01', '王市场', 2, '2023-09-01', 82, '市场活动执行力强'
    UNION ALL SELECT 24, NULL, 6, '美国客户经理',
           DATE_SUB(CURDATE(), INTERVAL 72 DAY), DATE_ADD(CURDATE(), INTERVAL 18 DAY),
           'admin', 1, NULL, NULL, NULL
) t
WHERE NOT EXISTS (SELECT 1 FROM hr_recruit_probation p WHERE p.employee_id = 24);

-- 让临期试用期与员工档案状态一致：24 号置为「试用期」
UPDATE sys_employee SET status = 2 WHERE id = 24 AND status = 1;

-- ============================================================================
-- 3. 审批中心：覆盖 审批中 / 已通过 / 已驳回 三态
--    审批人统一写 admin / hr001，用 admin 登录即可在「待我审批」看到 2 条
-- ============================================================================
INSERT INTO sys_approval_instance
    (biz_type, title, detail, applicant, applicant_name, status, current_node, node_count)
SELECT c1, c2, c3, c4, c5, c6, c7, c8
FROM (
    SELECT '请假' AS c1, '张总 年假 3 天' AS c2,
           '{"leaveType":"年假","startDate":"2026-10-13","endDate":"2026-10-15","days":3,"reason":"外出探亲"}' AS c3,
           'emp001' AS c4, '张总' AS c5, 1 AS c6, 2 AS c7, 2 AS c8
    UNION ALL SELECT '加班', '李技术 周末加班 6 小时',
           '{"reason":"上线割接","hours":6,"overtimeDate":"2026-10-18"}',
           'emp002', '李技术', 0, 1, 1
    UNION ALL SELECT '调薪', '申请陈开发月薪调整至 18000',
           '{"empId":10,"empName":"陈开发","newSalary":18000,"effectiveMonth":"2026-11","reason":"年度调薪"}',
           'emp001', '张总', 2, 2, 2
    UNION ALL SELECT '请假', '陈开发 病假 1 天',
           '{"leaveType":"病假","startDate":"2026-10-22","endDate":"2026-10-22","days":1,"reason":"感冒发烧"}',
           'emp003', '陈开发', 0, 1, 2
    UNION ALL SELECT '通用', '申请更换办公设备',
           '现有笔记本已使用 5 年，运行缓慢，申请更换',
           'emp002', '李技术', 1, 1, 1
) t
-- 守卫必须针对「本脚本要插入的这一批」，不能用 applicant='emp001'：
-- 既有库里 emp001 已有审批实例，会命中守卫导致 5 条演示实例整体不插入。
WHERE NOT EXISTS (SELECT 1 FROM sys_approval_instance i WHERE i.title = '张总 年假 3 天');

-- 审批记录通过 title 动态关联上面新插入的实例 id。
-- 原稿硬编码 instance_id = 1..5：既有库里 1..5 已被旧实例占用，记录会挂到别人的单子上；
-- 且守卫 r.instance_id = 1 早已命中，整段会被跳过。改为「按单号去重」后两者都解决。
INSERT INTO sys_approval_record
    (instance_id, node_order, approver, approver_name, status, comment, approve_time)
SELECT i.id, t.node_order, t.approver, t.approver_name, t.status, t.comment, t.approve_time
FROM (
    SELECT '张总 年假 3 天' AS title, 1 AS node_order, 'hr001' AS approver,
           '赵人事' AS approver_name, 1 AS status, '额度充足，同意' AS comment,
           '2026-10-09 09:12:00' AS approve_time
    UNION ALL SELECT '张总 年假 3 天', 2, 'admin', '系统管理员', 1, '同意', '2026-10-09 15:40:00'
    UNION ALL SELECT '李技术 周末加班 6 小时', 1, 'admin', '系统管理员', 0, NULL, NULL
    UNION ALL SELECT '申请陈开发月薪调整至 18000', 1, 'hr001', '赵人事', 1, '符合调薪区间', '2026-10-11 10:05:00'
    UNION ALL SELECT '申请陈开发月薪调整至 18000', 2, 'admin', '系统管理员', 2, '本轮预算已用尽，下季度再议', '2026-10-12 16:20:00'
    UNION ALL SELECT '陈开发 病假 1 天', 1, 'admin', '系统管理员', 0, NULL, NULL
    UNION ALL SELECT '陈开发 病假 1 天', 2, 'hr001', '赵人事', 0, NULL, NULL
    UNION ALL SELECT '申请更换办公设备', 1, 'admin', '系统管理员', 1, '同意，联系行政领取', '2026-10-20 11:30:00'
) t
JOIN sys_approval_instance i ON i.title = t.title
WHERE NOT EXISTS (SELECT 1 FROM sys_approval_record r WHERE r.instance_id = i.id);

-- ============================================================================
-- 4. 加班申请：状态 1 = 已通过，会参与薪资核算的加班费计算
-- ============================================================================
INSERT INTO sys_overtime (emp_id, overtime_date, hours, reason, status)
SELECT c1, c2, c3, c4, c5
FROM (
    SELECT 1 AS c1, DATE_SUB(CURDATE(), INTERVAL 12 DAY) AS c2, 3.0 AS c3,
           '客户方案评审' AS c4, 1 AS c5
    UNION ALL SELECT 2, DATE_SUB(CURDATE(), INTERVAL 10 DAY), 5.5, '版本上线割接', 1
    UNION ALL SELECT 10, DATE_SUB(CURDATE(), INTERVAL 8 DAY), 4.0, '需求联调', 1
    UNION ALL SELECT 12, DATE_SUB(CURDATE(), INTERVAL 6 DAY), 2.5, '缺陷修复', 1
    UNION ALL SELECT 14, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 6.0, '机房巡检', 0
    UNION ALL SELECT 15, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 4.0, '客户报价', 0
    UNION ALL SELECT 3,  DATE_SUB(CURDATE(), INTERVAL 2 DAY), 3.0, '展会筹备', 2
    UNION ALL SELECT 13, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 2.0, '测试用例补充', 1
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_overtime o WHERE o.emp_id = 1);

-- ============================================================================
-- 5. 系统公告
-- ============================================================================
INSERT INTO sys_notice (title, content, level, status, publisher)
SELECT c1, c2, c3, c4, c5
FROM (
    SELECT '2026 年度调薪工作的通知' AS c1,
           '各部门：2026 年度调薪工作将于 11 月启动，请以部门为单位在 11 月 10 日前提交调薪申请，逾期不再受理。' AS c2,
           'important' AS c3, 1 AS c4, 'admin' AS c5
    UNION ALL SELECT '关于启用新版考勤规则的通知',
           '自本月起，考勤状态统一按「正常/迟到/早退/缺勤/请假/出差」六类记录，请假审批通过后系统会自动写入考勤，不再按缺勤计扣工资。',
           'important', 1, 'admin'
    UNION ALL SELECT '国庆假期值班安排',
           '国庆期间各部门值班表已发布，请值班人员按时到岗并打卡。', 'normal', 1, 'admin'
    UNION ALL SELECT 'HRMS 系统上线公告',
           '人事管理系统已上线，员工可自助查看考勤、工资条与假期额度。', 'normal', 1, 'admin'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_notice n WHERE n.title = '2026 年度调薪工作的通知');

-- ============================================================================
-- 6. 站内信
-- ============================================================================
INSERT INTO sys_message (receiver, title, content, msg_type, sender, is_read)
SELECT c1, c2, c3, c4, c5, c6
FROM (
    SELECT 'admin' AS c1, '2 条审批待处理' AS c2, '您有 2 条待审批事项，请前往审批中心处理。' AS c3,
           '审批' AS c4, 'system' AS c5, 0 AS c6
    UNION ALL SELECT 'admin', '合同到期提醒', '有 3 份劳动合同将在 30 天内到期，1 份已过期，请及时处理。', '提醒', 'system', 0
    UNION ALL SELECT 'emp001', '年假申请已通过', '您提交的年假申请（3 天）已通过审批，年假额度已扣减。', '审批', 'system', 0
    UNION ALL SELECT 'emp001', '调薪申请被驳回', '您提交的调薪申请未通过：本轮预算已用尽，下季度再议。', '审批', 'system', 1
    UNION ALL SELECT 'emp002', '加班申请审批中', '您提交的加班申请已送达审批人，请耐心等待。', '审批', 'system', 0
    UNION ALL SELECT 'emp002', '设备更换申请已通过', '办公设备更换申请已通过，请联系行政领取。', '审批', 'system', 0
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_message m WHERE m.receiver = 'admin');

-- ============================================================================
-- 7. 物流轨迹：V1 里有 6 张发货单，但一张轨迹都没有
-- ============================================================================
INSERT INTO biz_shipment_track (shipment_id, status, node, location, content, track_time, operator)
SELECT c1, c2, c3, c4, c5, c6, c7
FROM (
    SELECT 1 AS c1, 1 AS c2, '已接单' AS c3, '深圳盐田港' AS c4, '发货单创建，等待仓库备货' AS c5,
           '2026-05-08 09:00:00' AS c6, 'admin' AS c7
    UNION ALL SELECT 1, 2, '已发运', '深圳盐田港', '货物已装船，开往洛杉矶港', '2026-05-10 18:20:00', 'admin'
    UNION ALL SELECT 1, 3, '清关中', '洛杉矶港', '抵达目的港，等待清关', '2026-05-26 10:15:00', 'Mike Johnson'
    UNION ALL SELECT 1, 4, '已签收', '洛杉矶', '客户已签收，回单已上传', '2026-05-28 16:40:00', 'Mike Johnson'
    UNION ALL SELECT 2, 1, '已接单', '深圳宝安机场', '航空货运单已生成', '2026-05-21 14:00:00', 'admin'
    UNION ALL SELECT 2, 2, '已发运', '深圳宝安机场', '航班起飞', '2026-05-22 20:05:00', 'admin'
    UNION ALL SELECT 2, 4, '已签收', '纽约肯尼迪机场', '客户已签收', '2026-05-25 11:30:00', 'Sarah Smith'
    UNION ALL SELECT 3, 1, '已接单', '深圳蛇口港', '发货单创建', '2026-06-10 09:30:00', 'admin'
    UNION ALL SELECT 3, 2, '已发运', '深圳蛇口港', '货物已装船', '2026-06-12 17:50:00', 'admin'
    UNION ALL SELECT 3, 3, '清关中', '西雅图港', '清关资料审核中', '2026-06-28 09:20:00', 'Mike Johnson'
    UNION ALL SELECT 3, 4, '已签收', '西雅图', '客户已签收', '2026-06-30 15:10:00', 'Mike Johnson'
    UNION ALL SELECT 4, 1, '已接单', '深圳盐田港', '发货单创建', '2026-07-06 10:00:00', 'admin'
    UNION ALL SELECT 4, 2, '已发运', '深圳盐田港', '货物已装船', '2026-07-08 19:30:00', 'admin'
    UNION ALL SELECT 4, 3, '清关中', '洛杉矶港', '清关中', '2026-07-24 08:45:00', 'Mike Johnson'
    UNION ALL SELECT 4, 4, '已签收', '洛杉矶', '客户已签收', '2026-07-26 14:00:00', 'Mike Johnson'
    UNION ALL SELECT 5, 1, '已接单', '深圳盐田港', '发货单创建，等待备货', '2026-08-10 09:15:00', 'admin'
    UNION ALL SELECT 5, 2, '运输中', '深圳盐田港', '货物已装船，预计 9 月 2 日抵达', '2026-08-12 18:00:00', 'admin'
    UNION ALL SELECT 6, 1, '已接单', '深圳', 'DHL 已揽收', '2026-09-14 15:20:00', 'admin'
    UNION ALL SELECT 6, 2, '运输中', '深圳', '已发出，转运中', '2026-09-15 10:05:00', 'admin'
    UNION ALL SELECT 6, 3, '清关中', '迈阿密', '目的国清关中', '2026-09-19 13:40:00', 'Sarah Smith'
) t
WHERE NOT EXISTS (SELECT 1 FROM biz_shipment_track s WHERE s.shipment_id = 1);

-- ============================================================================
-- 8. 库存流水：变动记录页此前 0 行
-- ============================================================================
INSERT INTO biz_inventory_log
    (product_id, warehouse_id, change_type, quantity, before_quantity, before_locked_quantity,
     after_quantity, after_locked_quantity, order_no, operator, remark, create_time)
SELECT c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12
FROM (
    SELECT 1 AS c1, 1 AS c2, 1 AS c3, 20000 AS c4, 30000 AS c5, 0 AS c6, 50000 AS c7, 0 AS c8,
           NULL AS c9, 'admin' AS c10, '月度补货入库' AS c11, DATE_SUB(NOW(), INTERVAL 20 DAY) AS c12
    UNION ALL SELECT 2, 1, 1, 8000, 12000, 0, 20000, 0, NULL, 'admin', '新品入库', DATE_SUB(NOW(), INTERVAL 18 DAY)
    UNION ALL SELECT 3, 3, 1, 3000, 5000, 0, 8000, 0, NULL, 'Mike Johnson', '海外仓补货', DATE_SUB(NOW(), INTERVAL 15 DAY)
    UNION ALL SELECT 1, 1, 3, 2000, 50000, 0, 50000, 2000, 'SO202609-001', 'admin', '订单锁定库存', DATE_SUB(NOW(), INTERVAL 12 DAY)
    UNION ALL SELECT 1, 1, 2, 2000, 50000, 2000, 48000, 0, 'SO202609-001', 'admin', '订单发货出库', DATE_SUB(NOW(), INTERVAL 10 DAY)
    UNION ALL SELECT 4, 2, 1, 1500, 0, 0, 1500, 0, NULL, 'admin', '义乌仓首期备货', DATE_SUB(NOW(), INTERVAL 9 DAY)
    UNION ALL SELECT 5, 1, 2, 300, 9000, 0, 8700, 0, NULL, 'admin', '样品出库', DATE_SUB(NOW(), INTERVAL 7 DAY)
    UNION ALL SELECT 3, 1, 2, 1200, 15000, 0, 13800, 0, 'SO202610-001', 'admin', '订单发货出库', DATE_SUB(NOW(), INTERVAL 5 DAY)
    UNION ALL SELECT 6, 2, 1, 2000, 500, 0, 2500, 0, NULL, 'admin', '补货入库', DATE_SUB(NOW(), INTERVAL 3 DAY)
    UNION ALL SELECT 2, 3, 2, 800, 5000, 200, 4200, 200, NULL, 'Mike Johnson', '海外仓调拨出库', DATE_SUB(NOW(), INTERVAL 1 DAY)
) t
WHERE NOT EXISTS (SELECT 1 FROM biz_inventory_log l WHERE l.product_id = 1);

-- ============================================================================
-- 9. 盘点单：1 张已确认（含差异）、1 张草稿
-- ============================================================================
INSERT INTO biz_stock_check
    (check_no, warehouse_id, check_date, status, item_count, diff_rows, diff_sum, operator, remark, confirm_time)
SELECT c1, c2, c3, c4, c5, c6, c7, c8, c9, c10
FROM (
    SELECT 'PD202610-0001' AS c1, 1 AS c2, DATE_SUB(CURDATE(), INTERVAL 6 DAY) AS c3, 2 AS c4,
           3 AS c5, 1 AS c6, -15 AS c7, 'admin' AS c8, '月度抽盘，1 项盘亏' AS c9,
           DATE_SUB(NOW(), INTERVAL 5 DAY) AS c10
    UNION ALL SELECT 'PD202610-0002', 3, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 1, 2, 0, 0,
           'Mike Johnson', '海外仓全盘草稿', NULL
) t
WHERE NOT EXISTS (SELECT 1 FROM biz_stock_check c WHERE c.check_no = 'PD202610-0001');

INSERT INTO biz_stock_check_item (check_id, product_id, book_quantity, actual_quantity, diff, remark)
SELECT c1, c2, c3, c4, c5, c6
FROM (
    SELECT 1 AS c1, 1 AS c2, 48000 AS c3, 47985 AS c4, -15 AS c5, '包装破损丢失' AS c6
    UNION ALL SELECT 1, 2, 20000, 20000, 0, NULL
    UNION ALL SELECT 1, 3, 13800, 13800, 0, NULL
    UNION ALL SELECT 2, 3, 8000, 8000, 0, '待复盘'
    UNION ALL SELECT 2, 4, 1200, 1200, 0, '待复盘'
) t
WHERE NOT EXISTS (SELECT 1 FROM biz_stock_check_item i WHERE i.check_id = 1);

-- ============================================================================
-- 10. 订单明细补漏：V1 里 biz_order 有 10 单，biz_order_item 只有 8 单的明细，
--     订单 9、10 点开是空的，取消/发货时库存联动也是空转
-- ============================================================================
INSERT INTO biz_order_item (order_id, product_id, warehouse_id, quantity, unit_price_usd, total_price_usd)
SELECT c1, c2, c3, c4, c5, c6
FROM (
    SELECT 9 AS c1, 2 AS c2, 1 AS c3, 10000 AS c4, 12.99 AS c5, 129900.00 AS c6
    UNION ALL SELECT 10, 1, 1, 25000, 2.99, 74750.00
) t
WHERE NOT EXISTS (SELECT 1 FROM biz_order_item i WHERE i.order_id = 9);
