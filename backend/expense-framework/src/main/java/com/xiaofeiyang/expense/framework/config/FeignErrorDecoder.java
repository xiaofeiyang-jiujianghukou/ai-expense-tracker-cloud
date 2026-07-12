package com.xiaofeiyang.expense.framework.config;

import com.xiaofeiyang.expense.framework.exception.FeignCallException;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;

import java.io.IOException;

/**
 * Global Feign ErrorDecoder — converts any non-2xx Feign response into
 * a {@link FeignCallException} with the full context:
 * <ul>
 *   <li>FeignClient class + method</li>
 *   <li>HTTP status code</li>
 *   <li>Response body (error message from downstream service)</li>
 * </ul>
 *
 * <p>Registered automatically for all {@code @FeignClient} interfaces.
 * Business code does NOT need try/catch for basic error visibility —
 * the exception bubbles up through {@code GlobalExceptionHandler}.</p>
 */
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        String body = "";
        try {
            if (response.body() != null) {
                body = Util.toString(response.body().asReader(Util.UTF_8));
            }
        } catch (IOException ignored) {
        }

        // methodKey format: "CategoryClient#findById(CategoryDetailRequest)"
        String client = methodKey;
        String method = methodKey;
        int hashIdx = methodKey.indexOf('#');
        if (hashIdx > 0) {
            client = methodKey.substring(0, hashIdx);
            method = methodKey.substring(hashIdx + 1);
        }

        return new FeignCallException(client, method, response.status(), body);
    }
}
