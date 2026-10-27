-- 绩效流程：自评 / 上级评分 / 面谈字段
-- 仅新增列，存量记录默认已完成，避免历史数据状态突变
ALTER TABLE sys_performance
    ADD COLUMN self_score DECIMAL(5,2) NULL COMMENT '自评总分',
    ADD COLUMN manager_score DECIMAL(5,2) NULL COMMENT '上级评分总分',
    ADD COLUMN self_summary VARCHAR(1000) NULL COMMENT '自我总结',
    ADD COLUMN improvement_plan VARCHAR(1000) NULL COMMENT '改进计划',
    ADD COLUMN interview_note VARCHAR(1000) NULL COMMENT '面谈记录',
    ADD COLUMN interview_time DATETIME NULL COMMENT '面谈时间',
    ADD COLUMN flow_status VARCHAR(20) NOT NULL DEFAULT 'completed'
        COMMENT 'pending_self/pending_manager/pending_interview/completed';
