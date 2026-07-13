package com.xiaofeiyang.expense.framework.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;

/**
 * Reads the X-User-Id header injected by the Gateway and sets
 * Spring Security's SecurityContextHolder for downstream use.
 *
 * <p>In the V4.0 microservices architecture, the Gateway validates JWT
 * and injects {@code X-User-Id} and {@code X-User-Email} headers.
 * This filter picks up those headers and populates the SecurityContext
 * so that existing code using {@code SecurityUtil.getCurrentUserId()}
 * continues to work without any changes.</p>
 *
 * <p>This filter should be registered in every downstream service.
 * It is safe even in the monolithic deployment because it only acts
 * when the X-User-Id header is present.</p>
 */
@Component
public class XUserFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(XUserFilter.class);

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID);

        if (userId != null) {
            try {
                Long id = Long.parseLong(userId);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                id, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("X-User-Id header found, set SecurityContext for userId={}", id);
            } catch (NumberFormatException e) {
                log.warn("Invalid X-User-Id header value: {}", userId);
            }
        }

        filterChain.doFilter(request, response);
    }
}
