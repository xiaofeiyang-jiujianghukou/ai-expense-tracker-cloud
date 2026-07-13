package com.xiaofeiyang.expense.framework.autoconfigure;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Provides MyBatis-Plus + DataSource + Flyway default settings —
 * activated ONLY when the service has declared mybatis-plus in its POM.
 *
 * <p>Runs before both {@link DataSourceAutoConfiguration} (to inject
 * {@code spring.datasource.*} defaults so Boot can create the DataSource)
 * and {@link MybatisPlusAutoConfiguration} (to supply MyBatis defaults).</p>
 *
 * <p>Services without MyBatis on classpath never load these properties
 * and never see any MyBatis-related warnings or errors.</p>
 */
@Configuration
@ConditionalOnClass(MybatisPlusAutoConfiguration.class)
@AutoConfigureBefore({DataSourceAutoConfiguration.class, MybatisPlusAutoConfiguration.class})
@PropertySource(value = "classpath:framework-mybatis-defaults.properties")
public class FrameworkMyBatisAutoConfiguration {
}
