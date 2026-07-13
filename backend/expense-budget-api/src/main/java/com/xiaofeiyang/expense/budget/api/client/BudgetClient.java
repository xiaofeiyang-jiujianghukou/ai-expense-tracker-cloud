package com.xiaofeiyang.expense.budget.api.client;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * Feign client for expense-budget service.
 * Reserved for future inter-service calls (currently no consumers).
 */
@FeignClient(
        name = "expense-budget",
        contextId = "expense-budget-api",
        path = "/api/budgets",
        url = "${budget-service-api.url:}"
)
public interface BudgetClient {
    // Feign methods to be added when consumers are identified
}
