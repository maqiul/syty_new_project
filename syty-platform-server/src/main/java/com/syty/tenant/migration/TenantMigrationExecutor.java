package com.syty.tenant.migration;

import java.util.List;
import java.util.Map;

public interface TenantMigrationExecutor {
    MigrationResult migrateTenant(String tenantId, String sourceDsKey, String targetDsKey);
    Map<String, MigrationResult> batchMigrate(List<String> tenantIds, String targetDsKey);

    record MigrationResult(String tenantId, boolean success, long affectedRows, String errorMessage) {}
}
