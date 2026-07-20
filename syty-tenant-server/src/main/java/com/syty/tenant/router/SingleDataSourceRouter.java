package com.syty.tenant.router;

import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;

@Component
public class SingleDataSourceRouter implements TenantDataSourceRouter {

    private final DataSource defaultDataSource;

    public SingleDataSourceRouter(DataSource defaultDataSource) {
        this.defaultDataSource = defaultDataSource;
    }

    @Override
    public DataSource route(String tenantId) {
        return defaultDataSource;
    }

    @Override
    public Map<String, DataSource> getAllDataSources() {
        return Collections.singletonMap("default", defaultDataSource);
    }
}
