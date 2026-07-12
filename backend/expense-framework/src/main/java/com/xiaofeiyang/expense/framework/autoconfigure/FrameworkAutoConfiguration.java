package com.xiaofeiyang.expense.framework.autoconfigure;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

/**
 * Main auto-configuration entry point for expense-framework.
 *
 * <p>Component-scans the framework package to pick up @Component beans
 * and imports sub-configurations. Only {@code framework-defaults.properties}
 * is always loaded (Actuator — every service needs it). All other
 * components (MyBatis, Redis, DataSource) activate via
 * {@code @ConditionalOnClass} — "引了就说明需要，没引就是不需要".</p>
 */
@Configuration
@ComponentScan(basePackages = "com.xiaofeiyang.expense.framework")
@PropertySource("classpath:framework-defaults.properties")
@Import({
        FrameworkSecurityAutoConfiguration.class,
        FrameworkFeignAutoConfiguration.class,
        FrameworkWebAutoConfiguration.class,
        FrameworkMyBatisAutoConfiguration.class,
        FrameworkDataSourceAutoConfiguration.class,
        FrameworkRedisAutoConfiguration.class,
})
public class FrameworkAutoConfiguration {
}
