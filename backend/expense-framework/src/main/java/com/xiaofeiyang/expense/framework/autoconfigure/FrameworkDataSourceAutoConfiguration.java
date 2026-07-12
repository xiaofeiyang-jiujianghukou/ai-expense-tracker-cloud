package com.xiaofeiyang.expense.framework.autoconfigure;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Provides a default MySQL DataSource from environment variables.
 *
 * <p>Activated only when HikariDataSource is on the classpath (i.e., when
 * the service includes MyBatis-Plus / MySQL connector). If the service
 * provides its own DataSource bean, this one is skipped.</p>
 *
 * <p>Expected environment variables:
 * <ul>
 *   <li>{@code DB_HOST} — MySQL host (default: localhost)</li>
 *   <li>{@code DB_PORT} — MySQL port (default: 3306)</li>
 *   <li>{@code EXPENSE_DB_USERNAME} — DB username</li>
 *   <li>{@code EXPENSE_DB_PASSWORD} — DB password</li>
 * </ul>
 */
@Configuration
@ConditionalOnClass(HikariDataSource.class)
public class FrameworkDataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.datasource.url", havingValue = "false", matchIfMissing = true)
    public DataSource dataSource() {
        String host = env("DB_HOST", "localhost");
        String port = env("DB_PORT", "3306");
        String user = env("EXPENSE_DB_USERNAME", "root");
        String pass = env("EXPENSE_DB_PASSWORD", "root");

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(String.format(
                "jdbc:mysql://%s:%s/ai_expense_tracker?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai",
                host, port));
        ds.setUsername(user);
        ds.setPassword(pass);
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return ds;
    }

    private static String env(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }
}
