package com.xiaofeiyang.expense.user.api.client;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * Feign client for expense-user service.
 * Reserved for future inter-service calls (currently no consumers).
 */
@FeignClient(
        name = "expense-user",
        contextId = "expense-user-api",
        path = "/api/users",
        url = "${user-service-api.url:}"
)
public interface UserClient {
    // Feign methods to be added when consumers are identified
}
