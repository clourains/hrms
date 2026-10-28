-- 修正入离管理演示数据：LZ20261001 原指向 EMP001（张总），
-- 而该员工绑定了管理员登录账号，点「办理生效」会一并停用管理员账号，导致后台无法登录。
-- 这里把演示单据改挂到「没有绑定任何登录账号」的在职员工上，保证演示安全。
UPDATE hr_employee_exit x
JOIN (
    SELECT e.id, e.emp_no, e.name, e.dept_id, e.position
    FROM sys_employee e
    WHERE e.deleted = 0
      AND e.status <> 0
      AND NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.emp_id = e.id)
    ORDER BY e.id
    LIMIT 1
) e ON 1 = 1
SET x.employee_id = e.id,
    x.emp_no = e.emp_no,
    x.employee_name = e.name,
    x.dept_id = e.dept_id,
    x.position = e.position
WHERE x.exit_no = 'LZ20261001'
  AND x.status = 1
  AND x.deleted = 0;
