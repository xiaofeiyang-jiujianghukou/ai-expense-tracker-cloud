package com.xiaofeiyang.expense.framework.enums;

import java.util.List;

/**
 * Bill type: INCOME or EXPENSE.
 * Named BillType (not TransactionType) to avoid confusion with DB transactions or bank transactions.
 * Each type carries its default category names for new-user initialization.
 */
public enum BillType {
    INCOME(List.of("工资", "奖金", "投资")),
    EXPENSE(List.of("餐饮", "交通", "购物", "娱乐", "住房"));

    private final List<String> defaultCategories;

    BillType(List<String> defaultCategories) {
        this.defaultCategories = defaultCategories;
    }

    public List<String> getDefaultCategories() {
        return defaultCategories;
    }
}
