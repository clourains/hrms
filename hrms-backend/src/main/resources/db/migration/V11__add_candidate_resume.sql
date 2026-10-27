-- 候选人简历附件
-- 简历以文件形式存放在后端本地目录（默认 hrms-backend/uploads/resume/yyyy/MM/uuid.ext），
-- 库里只保存相对路径与原始文件名，文件本身不入库、不进 Git。
ALTER TABLE hr_recruit_candidate
    ADD COLUMN resume_url  VARCHAR(500) NULL COMMENT '简历文件相对路径，如 resume/2026/10/xxx.jpg',
    ADD COLUMN resume_name VARCHAR(255) NULL COMMENT '简历原始文件名，下载时还原',
    ADD COLUMN resume_size BIGINT       NULL COMMENT '简历文件大小（字节）',
    ADD COLUMN resume_type VARCHAR(30)  NULL COMMENT '简历文件归类：image / pdf';
