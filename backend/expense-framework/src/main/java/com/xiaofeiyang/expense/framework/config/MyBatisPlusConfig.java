package com.xiaofeiyang.expense.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus pagination plugin — activated only when BOTH
 * {@code MybatisPlusInterceptor} AND {@code PaginationInnerInterceptor}
 * are on the classpath (i.e. the service has declared both
 * {@code mybatis-plus-spring-boot3-starter} AND {@code mybatis-plus-jsqlparser}).
 *
 * <p>Without the dual check, a service that has only the starter (without
 * jsqlparser) would pass {@code @ConditionalOnClass} but fail at runtime
 * with {@code NoClassDefFoundError: PaginationInnerInterceptor}.
 */
@Configuration
@ConditionalOnClass({MybatisPlusInterceptor.class, PaginationInnerInterceptor.class})
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
