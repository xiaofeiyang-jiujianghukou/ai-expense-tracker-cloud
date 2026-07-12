package com.xiaofeiyang.expense.framework.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaofeiyang.expense.framework.config.ApiResponseDecoder;
import com.xiaofeiyang.expense.framework.config.FeignErrorDecoder;
import com.xiaofeiyang.expense.framework.config.UserContextFeignInterceptor;
import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Framework-level Feign infrastructure — all beans are global,
 * no per-{@code @FeignClient} configuration needed.
 *
 * <ul>
 *   <li>{@link ApiResponseDecoder} — unwraps {@code ApiResponse.data} from Feign responses</li>
 *   <li>{@link UserContextFeignInterceptor} — propagates X-User-Id</li>
 *   <li>{@link FeignErrorDecoder} — converts Feign errors into
 *       {@link com.xiaofeiyang.expense.framework.exception.FeignCallException}
 *       with full context (client, method, status, response body)</li>
 *   <li>{@link Logger.Level#FULL} — HTTP request/response logging</li>
 * </ul>
 */
@Configuration
@ConditionalOnClass(RequestInterceptor.class)
public class FrameworkFeignAutoConfiguration {

    @Bean
    @Primary
    public Decoder apiResponseDecoder(ObjectProvider<HttpMessageConverters> converters,
                                      ObjectMapper objectMapper) {
        return new ApiResponseDecoder(converters, objectMapper);
    }

    @Bean
    public UserContextFeignInterceptor userContextFeignInterceptor() {
        return new UserContextFeignInterceptor();
    }

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new FeignErrorDecoder();
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
