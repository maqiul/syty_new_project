package com.syty.tenant.query;

import java.util.List;

public interface TenantQueryAggregator {
    <T> List<T> aggregateQuery(String queryKey, Object params, List<String> tenantIds);
    <T> T aggregateStat(String queryKey, Object params);
}
