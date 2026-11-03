package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class InMemoryResourceStoreTest {
    @Test
    void createsUniqueResources() {
        InMemoryResourceStore store = new InMemoryResourceStore();
        Resource resource = new Resource("room-1", 25);

        assertSame(resource, store.create(resource));
        ResponseStatusException conflict = assertThrows(
                ResponseStatusException.class, () -> store.create(resource));
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
    }
}
