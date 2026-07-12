-- V5: Rename table "transaction" → "bill" and column "transaction_date" → "bill_date"
-- to avoid confusion with database transactions.
ALTER TABLE transaction RENAME TO bill;
ALTER TABLE bill CHANGE COLUMN transaction_date bill_date DATE NOT NULL COMMENT '账单日期';
