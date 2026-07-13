CREATE TABLE transaction (
    id               BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id          BIGINT          NOT NULL               COMMENT '用户ID',
    category_id      BIGINT          NOT NULL               COMMENT '分类ID',
    amount           DECIMAL(12,2)   NOT NULL               COMMENT '金额',
    type             VARCHAR(20)     NOT NULL               COMMENT '交易类型：INCOME=收入 EXPENSE=支出',
    description      VARCHAR(500)    DEFAULT NULL           COMMENT '备注',
    transaction_date DATE            NOT NULL               COMMENT '交易日期',
    created_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_user_date (user_id, transaction_date),
    INDEX idx_user_type (user_id, type),
    INDEX idx_user_category (user_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单表';
