-- V4: Add composite index for statistics aggregation queries.
-- Covers WHERE user_id = ? AND type = ? AND transaction_date BETWEEN ? AND ?
-- Used by StatisticsService.sumByType() and sumByCategory().
CREATE INDEX idx_user_type_date ON transaction (user_id, type, transaction_date);
