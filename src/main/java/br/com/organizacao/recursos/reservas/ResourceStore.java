package br.com.organizacao.recursos.reservas;

import java.util.List;
import java.util.Optional;

public interface ResourceStore {
    Resource create(Resource resource);

    Optional<Resource> findById(String id);

    List<Resource> search(ResourceFilter filter);

    Resource update(Resource resource);
}
