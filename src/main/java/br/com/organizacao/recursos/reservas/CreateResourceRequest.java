package br.com.organizacao.recursos.reservas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateResourceRequest(
        @NotBlank(message = "id is required") String id,
        @NotNull(message = "capacity is required")
        @Positive(message = "capacity must be greater than zero") Integer capacity) {
}
