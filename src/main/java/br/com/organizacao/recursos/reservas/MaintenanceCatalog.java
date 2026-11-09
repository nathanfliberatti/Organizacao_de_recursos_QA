package br.com.organizacao.recursos.reservas;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceCatalog {
    private final Map<String, MaintenancePeriod> periodsByResource = new ConcurrentHashMap<>();

    public void block(String resourceId, MaintenancePeriod period) {
        periodsByResource.put(resourceId, period);
    }

    public MaintenancePeriod findByResourceId(String resourceId) {
        return periodsByResource.get(resourceId);
    }

    public void remove(String resourceId) {
        periodsByResource.remove(resourceId);
    }

    public Map<String, MaintenancePeriod> all() {
        return new TreeMap<>(periodsByResource);
    }

    public void clear() {
        periodsByResource.clear();
    }
}
