package br.com.organizacao.recursos.reservas;

import java.time.Instant;

/** Visao de listagem: situacao de agenda (AGENDADA/CANCELADA) e estado do ciclo de vida (RF-14). */
public record ReservationSummary(
        String id,
        String resourceId,
        String professorId,
        String requesterId,
        Instant startsAt,
        Instant endsAt,
        String schedulingStatus,
        String lifecycleStatus) {
    public ReservationSummary withLifecycleStatus(String status) {
        return new ReservationSummary(id, resourceId, professorId, requesterId, startsAt, endsAt,
                schedulingStatus, status);
    }
}
