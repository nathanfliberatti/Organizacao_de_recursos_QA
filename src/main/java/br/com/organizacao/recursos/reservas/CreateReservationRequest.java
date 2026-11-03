package br.com.organizacao.recursos.reservas;

import java.time.Instant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(
        @NotBlank(message = "resourceId is required") String resourceId,
        @NotNull(message = "startsAt is required") Instant startsAt,
        @NotNull(message = "endsAt is required") Instant endsAt,
        String professorId) {
	public CreateReservationRequest(String resourceId, Instant startsAt, Instant endsAt) {
		this(resourceId, startsAt, endsAt, null);
	}
}
