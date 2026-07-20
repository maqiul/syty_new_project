package com.syty.tenant.migration;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class NoOpMigrationExecutor implements TenantMigrationExecutor {

    @Override
    public MigrationResult migrateTenant(String tenantId, String sourceDsKey, String targetDsKey) {
        return new MigrationResult(tenantId, true, 0, null);
    }

    @Override
    public Map<String, MigrationResult> batchMigrate(List<String> tenantIds, String targetDsKey) {
        return tenantIds.stream()
            .collect(Collectors.toMap(id -> id, id -> migrateTenant(id, "default", targetDsKey)));
    }
}
