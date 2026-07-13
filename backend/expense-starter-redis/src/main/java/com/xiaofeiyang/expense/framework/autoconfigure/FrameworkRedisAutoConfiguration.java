package com.xiaofeiyang.expense.framework.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Provides Redis connection defaults from environment variables.
 *
 * <p>Activated only when Redis is on the classpath. Environment variables:
 * <ul>
 *   <li>{@code REDIS_HOST} (default: localhost)</li>
 *   <li>{@code REDIS_PORT} (default: 6379)</li>
 *   <li>{@code REDIS_PASSWORD} (default: empty)</li>
 * </ul>
 */
@Configuration
@ConditionalOnClass(RedisTemplate.class)
@PropertySource(value = "classpath:framework-redis-defaults.properties", ignoreResourceNotFound = true)
public class FrameworkRedisAutoConfiguration {
}
