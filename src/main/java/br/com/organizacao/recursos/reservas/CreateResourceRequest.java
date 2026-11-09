package br.com.organizacao.recursos.reservas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateResourceRequest(
        @NotBlank(message = "id is required") String id,
        @NotNull(message = "capacity is required")
        @Positive(message = "capacity must be greater than zero") Integer capacity,
        String name,
        ResourceType type,
        String location,
        String competency,
        Boolean restricted) {
    public CreateResourceRequest(String id, Integer capacity) {
        this(id, capacity, null, null, null, null, null);
    }

    public Resource toResource() {
        return new Resource(id, name, type, capacity, blankToNull(location), blankToNull(competency),
                Boolean.TRUE.equals(restricted));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
