-- 导入/开放接口调用记录保留失败明细，便于对接方排查（Excel 与 API 共用）
ALTER TABLE `sys_import_log`
    ADD COLUMN `errors` text NULL COMMENT '失败明细（每行一条，最多 20 条）' AFTER `failed`;
