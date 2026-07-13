package com.xiaofeiyang.expense.framework.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign RequestInterceptor that propagates X-User-Id and X-User-Email headers
 * from the incoming request to outgoing Feign calls.
 *
 * <p>When Gateway authenticates a request and injects these headers, downstream
 * services must forward them when calling other services via Feign. This
 * interceptor reads the headers from the current HTTP request and copies them
 * to the Feign request.</p>
 */
public class UserContextFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }

        HttpServletRequest request = attrs.getRequest();
        String userId = request.getHeader("X-User-Id");
        String email = request.getHeader("X-User-Email");

        if (userId != null) {
            template.header("X-User-Id", userId);
        }
        if (email != null) {
            template.header("X-User-Email", email);
        }
    }
}
