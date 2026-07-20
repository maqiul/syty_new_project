package com.syty.tenant.router;

import javax.sql.DataSource;
import java.util.Map;

public interface TenantDataSourceRouter {
    DataSource route(String tenantId);
    Map<String, DataSource> getAllDataSources();
}
