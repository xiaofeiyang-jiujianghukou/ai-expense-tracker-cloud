-- V6: Budget table for per-category monthly budget targets.
CREATE TABLE budget (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id       BIGINT          NOT NULL               COMMENT '用户ID',
    category_id   BIGINT          NOT NULL               COMMENT '分类ID',
    year          INT             NOT NULL               COMMENT '年份',
    month         INT             NOT NULL               COMMENT '月份',
    target_amount DECIMAL(12,2)   NOT NULL               COMMENT '预算目标金额',
    created_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_cat_month (user_id, category_id, year, month),
    INDEX idx_user_month (user_id, year, month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预算表';
