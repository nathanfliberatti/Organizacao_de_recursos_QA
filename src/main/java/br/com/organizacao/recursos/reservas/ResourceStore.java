package br.com.organizacao.recursos.reservas;

import java.util.List;

public interface ResourceStore {
    Resource create(Resource resource);

    List<Resource> findAll();
}
