-- P6-1 公告栏：管理员发布公告，首页置顶展示，支持已读统计
CREATE TABLE IF NOT EXISTS sys_notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content TEXT COMMENT '公告内容',
    level VARCHAR(20) NOT NULL DEFAULT 'normal' COMMENT '级别：normal普通 / important重要',
    status INT NOT NULL DEFAULT 1 COMMENT '状态：1发布 0下线',
    publisher VARCHAR(50) DEFAULT NULL COMMENT '发布人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告';

-- 已读记录：一人一公告一条，统计已读人数
CREATE TABLE IF NOT EXISTS sys_notice_read (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notice_id BIGINT NOT NULL COMMENT '公告ID',
    username VARCHAR(50) NOT NULL COMMENT '已读用户',
    read_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    UNIQUE KEY uk_notice_user (notice_id, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告已读记录';

-- P6-3 SMTP 邮件参数（默认关闭，配置后工资条推送时同步发邮件）
INSERT IGNORE INTO sys_config (config_key, config_value, remark) VALUES
('mail.enabled', 'false', 'SMTP邮件开关：true开启 / false关闭（未开启时自动跳过发信）'),
('mail.host', '', 'SMTP服务器地址，如 smtp.qq.com / smtp.163.com'),
('mail.port', '465', 'SMTP端口（SSL 一般 465，TLS 一般 587）'),
('mail.username', '', 'SMTP账号（发件邮箱）'),
('mail.password', '', 'SMTP密码/授权码');
