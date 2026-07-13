package com.xiaofeiyang.expense.ai.api.client;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * Feign client for expense-ai service.
 * Reserved for future inter-service calls (currently no consumers).
 */
@FeignClient(
        name = "expense-ai",
        contextId = "expense-ai-api",
        path = "/api/ai",
        url = "${ai-service-api.url:}"
)
public interface AiClient {
    // Feign methods to be added when consumers are identified
}
