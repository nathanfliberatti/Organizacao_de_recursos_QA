package br.com.organizacao.recursos.reservas;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
@Profile("!postgres")
public class InMemoryResourceStore implements ResourceStore {
    private final ConcurrentMap<String, Resource> resources = new ConcurrentHashMap<>();

    @Override
    public Resource create(Resource resource) {
        if (resources.putIfAbsent(resource.id(), resource) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Resource already exists");
        }
        return resource;
    }

    @Override
    public Optional<Resource> findById(String id) {
        return Optional.ofNullable(resources.get(id));
    }

    @Override
    public List<Resource> search(ResourceFilter filter) {
        return resources.values().stream()
                .filter(filter::matches)
                .sorted(Comparator.comparing(Resource::id))
                .toList();
    }

    @Override
    public Resource update(Resource resource) {
        if (resources.replace(resource.id(), resource) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
        return resource;
    }
}
