package br.com.organizacao.recursos.reservas;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MaintenanceCatalog {
    private final Map<String, MaintenancePeriod> periodsByResource = new ConcurrentHashMap<>();

    public void block(String resourceId, MaintenancePeriod period) {
        periodsByResource.put(resourceId, period);
    }

    public MaintenancePeriod findByResourceId(String resourceId) {
        return periodsByResource.get(resourceId);
    }
}
