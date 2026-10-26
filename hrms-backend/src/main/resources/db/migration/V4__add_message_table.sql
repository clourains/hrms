-- P2-1 站内信：系统/审批/提醒类消息统一走 sys_message
CREATE TABLE IF NOT EXISTS sys_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receiver VARCHAR(50) NOT NULL COMMENT '接收人用户名（sys_user.username）',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT COMMENT '内容',
    msg_type VARCHAR(30) NOT NULL DEFAULT '系统' COMMENT '类型：系统/审批/提醒',
    sender VARCHAR(50) NOT NULL DEFAULT 'system' COMMENT '发送人用户名，system 表示系统消息',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    read_time DATETIME DEFAULT NULL COMMENT '阅读时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_message_receiver (receiver),
    KEY idx_message_unread (receiver, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信';
