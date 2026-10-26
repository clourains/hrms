-- P5-2 登录日志：记录登录成功/失败，用于安全追溯与异常排查
CREATE TABLE IF NOT EXISTS sys_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '登录用户名',
    success TINYINT NOT NULL DEFAULT 0 COMMENT '0失败 1成功',
    msg VARCHAR(255) DEFAULT NULL COMMENT '结果说明/失败原因',
    ip VARCHAR(50) DEFAULT NULL COMMENT '客户端IP',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    KEY idx_login_username (username, create_time),
    KEY idx_login_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';
