CREATE TABLE category (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id      BIGINT       NOT NULL               COMMENT '用户ID',
    name         VARCHAR(100) NOT NULL               COMMENT '分类名称',
    type         VARCHAR(20)  NOT NULL               COMMENT '分类类型：INCOME=收入 EXPENSE=支出',
    created_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_user_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';
