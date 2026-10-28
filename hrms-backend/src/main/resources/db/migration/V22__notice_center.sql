-- 公告中心升级：发布范围、生效时间与置顶
-- scope_type=0 全员；=1 仅指定部门（dept_ids 逗号分隔，用 FIND_IN_SET 匹配）
-- effective_start/end 为空表示立即生效 / 长期有效
ALTER TABLE sys_notice
    ADD COLUMN scope_type TINYINT NOT NULL DEFAULT 0 COMMENT '0全员 1指定部门' AFTER status,
    ADD COLUMN dept_ids VARCHAR(255) NULL COMMENT '指定部门ID，逗号分隔' AFTER scope_type,
    ADD COLUMN effective_start DATETIME NULL COMMENT '生效开始时间，空=立即' AFTER dept_ids,
    ADD COLUMN effective_end DATETIME NULL COMMENT '生效结束时间，空=长期' AFTER effective_start,
    ADD COLUMN pinned TINYINT NOT NULL DEFAULT 0 COMMENT '1置顶' AFTER effective_end;

-- 演示：把“系统升级通知”这类全员公告置顶，便于验证排序
UPDATE sys_notice SET pinned = 1 WHERE level = 'important' ORDER BY id DESC LIMIT 1;
