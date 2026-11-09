package br.com.organizacao.recursos.reservas;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResourceController {
    private final ResourceStore resourceStore;

    public ResourceController(ResourceStore resourceStore) {
        this.resourceStore = resourceStore;
    }

    @PostMapping("/api/resources")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResourceResponse> create(@Valid @RequestBody CreateResourceRequest request) {
        Resource resource = resourceStore.create(request.toResource());
        return ResponseEntity.status(HttpStatus.CREATED).body(ResourceResponse.from(resource));
    }

    @PutMapping("/api/resources/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResourceResponse update(@PathVariable String id, @Valid @RequestBody CreateResourceRequest request) {
        Resource resource = request.toResource();
        return ResourceResponse.from(resourceStore.update(new Resource(id, resource.name(), resource.type(),
                resource.capacity(), resource.location(), resource.competency(), resource.restricted())));
    }
}
