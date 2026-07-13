package com.xiaofeiyang.expense.framework.exception;

/**
 * Thrown automatically when a Feign call fails — contains the full context
 * for debugging without needing per-call try/catch in business code.
 *
 * <p>Handled by {@code GlobalExceptionHandler} with a clear error response.</p>
 */
public class FeignCallException extends BusinessException {

    private final String feignClient;
    private final String methodKey;
    private final int httpStatus;

    public FeignCallException(String feignClient, String methodKey, int httpStatus, String responseBody) {
        super(ErrorCode.FEIGN_CALL_FAILED.getCode(),
                buildMessage(feignClient, methodKey, httpStatus, responseBody));
        this.feignClient = feignClient;
        this.methodKey = methodKey;
        this.httpStatus = httpStatus;
    }

    private static String buildMessage(String client, String method, int status, String body) {
        return String.format("[%s] %s -> HTTP %d, response: %s", client, method, status, body);
    }

    public String getFeignClient() { return feignClient; }
    public String getMethodKey() { return methodKey; }
    public int getHttpStatus() { return httpStatus; }
}
