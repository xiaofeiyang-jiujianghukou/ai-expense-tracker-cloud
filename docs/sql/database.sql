-- 1. 创建数据库
CREATE DATABASE `ai_expense_tracker` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 创建用户（MySQL 8.0 要求先创建用户再授权）
CREATE USER 'ai-expense-tracker'@'%' IDENTIFIED BY 'xfylovesxy';

-- 3. 授权用户拥有该数据库的所有权限
GRANT ALL PRIVILEGES ON `ai_expense_tracker`.* TO 'ai-expense-tracker'@'%';

-- 4. 刷新权限使生效
FLUSH PRIVILEGES;