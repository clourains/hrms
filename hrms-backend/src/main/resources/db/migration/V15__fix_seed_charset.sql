-- 修正 V14 种子数据乱码
-- 原因：V14 使用 INSERT ... SELECT ... UNION ALL 派生表，MySQL 对 UNION 列按连接字符集推导，
-- 导致中文字面量被二次编码写入。此处按 id + 业务类型精确回写正确值。

UPDATE sys_performance_indicator SET indicator_name = '工作业绩', description = '工作目标完成情况、工作质量和效率'
WHERE id = 1 AND weight = 40;

UPDATE sys_performance_indicator SET indicator_name = '工作能力', description = '专业技能、学习能力、问题解决能力'
WHERE id = 2 AND weight = 25;

UPDATE sys_performance_indicator SET indicator_name = '工作态度', description = '责任心、积极性、团队协作'
WHERE id = 3 AND weight = 20;

UPDATE sys_performance_indicator SET indicator_name = '创新改进', description = '创新意识、流程优化、知识分享'
WHERE id = 4 AND weight = 15;

UPDATE sys_salary_structure SET item_name = '基本工资'
WHERE id = 1 AND item_type = 'base';

UPDATE sys_salary_structure SET item_name = '绩效奖金'
WHERE id = 2 AND item_type = 'bonus';

UPDATE sys_salary_structure SET item_name = '社保个人部分'
WHERE id = 3 AND item_type = 'deduction';

UPDATE sys_salary_structure SET item_name = '公积金个人部分'
WHERE id = 4 AND item_type = 'deduction';

UPDATE sys_salary_structure SET item_name = '个人所得税'
WHERE id = 5 AND item_type = 'deduction';
