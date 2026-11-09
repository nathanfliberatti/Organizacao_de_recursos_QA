package br.com.organizacao.recursos.reservas;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Consulta e pesquisa de recursos (RF-02, RF-03). */
@RestController
public class ResourceSearchController {
    private final ResourceStore resourceStore;
    private final ReservationSchedulingService schedulingService;

    public ResourceSearchController(ResourceStore resourceStore, ReservationSchedulingService schedulingService) {
        this.resourceStore = resourceStore;
        this.schedulingService = schedulingService;
    }

    @GetMapping("/api/resources")
    @PreAuthorize("isAuthenticated()")
    public List<ResourceResponse> search(
            @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String competency,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) Instant availableFrom,
            @RequestParam(required = false) Instant availableTo) {
        if ((availableFrom == null) != (availableTo == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "availableFrom and availableTo must be informed together");
        }
        List<Resource> found = resourceStore.search(new ResourceFilter(type, minCapacity, location, competency, text));
        if (availableFrom == null) {
            return found.stream().map(ResourceResponse::from).toList();
        }
        if (!availableFrom.isBefore(availableTo)) {
            throw new InvalidReservationPeriodException();
        }
        ReservationPeriod period = new ReservationPeriod(availableFrom, availableTo);
        return found.stream()
                .filter(resource -> schedulingService.isAvailable(
                        resource.id(), resource.type() == ResourceType.PROFESSOR ? resource.id() : null, period))
                .map(ResourceResponse::from)
                .toList();
    }

    @GetMapping("/api/resources/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResourceResponse find(@PathVariable String id) {
        return resourceStore.findById(id).map(ResourceResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
    }
}
