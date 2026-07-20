package com.syty.tenant.query;

import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
public class SingleSourceQueryAggregator implements TenantQueryAggregator {

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> aggregateQuery(String queryKey, Object params, List<String> tenantIds) {
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T aggregateStat(String queryKey, Object params) {
        return null;
    }
}
