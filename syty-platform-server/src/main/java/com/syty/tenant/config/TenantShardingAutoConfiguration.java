package com.syty.tenant.config;

import com.syty.tenant.migration.NoOpMigrationExecutor;
import com.syty.tenant.migration.TenantMigrationExecutor;
import com.syty.tenant.query.SingleSourceQueryAggregator;
import com.syty.tenant.query.TenantQueryAggregator;
import com.syty.tenant.router.SingleDataSourceRouter;
import com.syty.tenant.router.TenantDataSourceRouter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class TenantShardingAutoConfiguration {

    @Bean
    public TenantDataSourceRouter tenantDataSourceRouter(DataSource dataSource) {
        return new SingleDataSourceRouter(dataSource);
    }

    @Bean
    public TenantMigrationExecutor tenantMigrationExecutor() {
        return new NoOpMigrationExecutor();
    }

    @Bean
    public TenantQueryAggregator tenantQueryAggregator() {
        return new SingleSourceQueryAggregator();
    }
}
