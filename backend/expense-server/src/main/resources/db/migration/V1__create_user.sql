CREATE TABLE `user` (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    email        VARCHAR(255) NOT NULL               COMMENT '邮箱',
    password     VARCHAR(255) NOT NULL               COMMENT '密码（BCrypt加密）',
    nickname     VARCHAR(100) DEFAULT NULL           COMMENT '昵称',
    status       TINYINT      NOT NULL DEFAULT 1     COMMENT '状态：1=正常 0=禁用',
    created_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
