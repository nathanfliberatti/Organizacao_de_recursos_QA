package br.com.organizacao.recursos.reservas;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResourceController {
    private final ResourceStore resourceStore;

    public ResourceController(ResourceStore resourceStore) {
        this.resourceStore = resourceStore;
    }

    @GetMapping("/api/resources")
    public java.util.List<ResourceResponse> list() {
        return resourceStore.findAll().stream()
                .map(resource -> new ResourceResponse(resource.id(), resource.capacity()))
                .toList();
    }

    @PostMapping("/api/resources")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ResourceResponse> create(@Valid @RequestBody CreateResourceRequest request) {
        Resource resource = resourceStore.create(new Resource(request.id(), request.capacity()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResourceResponse(resource.id(), resource.capacity()));
    }
}
