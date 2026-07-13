package com.xiaofeiyang.expense.framework.autoconfigure;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Main auto-configuration entry point for expense-starter-web.
 *
 * <p>Component-scans the framework package to pick up @Component / @Configuration
 * beans (Security, Feign, Web auto-config classes + shared singletons).
 * Only {@code framework-defaults.properties} is always loaded (Nacos, Feign,
 * Actuator — every application service needs these).</p>
 *
 * <p>ORM and Redis auto-configuration are registered independently by their
 * own starters via {@code AutoConfiguration.imports} — no cross-module
 * {@code @Import} needed.</p>
 */
@Configuration
@ComponentScan(basePackages = "com.xiaofeiyang.expense.framework")
@PropertySource("classpath:framework-defaults.properties")
public class FrameworkAutoConfiguration {
}
