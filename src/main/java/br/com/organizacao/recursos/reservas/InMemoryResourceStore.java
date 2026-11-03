package br.com.organizacao.recursos.reservas;

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
}
