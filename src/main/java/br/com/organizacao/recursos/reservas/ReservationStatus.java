package br.com.organizacao.recursos.reservas;

/**
 * Estados do ciclo de vida da reserva (RF-14).
 *
 * <pre>
 * SOLICITADA → APROVADA → EM_USO → CONCLUIDA        (fluxo principal)
 * SOLICITADA → REJEITADA                            (Responsável rejeita)
 * SOLICITADA | APROVADA → CANCELADA                 (dono, Responsável ou Administrador cancela)
 * APROVADA → NAO_COMPARECEU                         (Responsável ou Administrador registra a ausência)
 * </pre>
 *
 * CONCLUIDA, REJEITADA, CANCELADA e NAO_COMPARECEU são estados finais. Uma reserva iniciada
 * (EM_USO ou CONCLUIDA) nunca pode ser cancelada nem apagada (RF-15).
 */
public enum ReservationStatus {
    SOLICITADA,
    APROVADA,
    EM_USO,
    CONCLUIDA,
    REJEITADA,
    CANCELADA,
    NAO_COMPARECEU;

    public boolean canMoveTo(ReservationStatus next) {
        return switch (this) {
            case SOLICITADA -> next == APROVADA || next == REJEITADA || next == CANCELADA;
            case APROVADA -> next == EM_USO || next == CANCELADA || next == NAO_COMPARECEU;
            case EM_USO -> next == CONCLUIDA;
            case CONCLUIDA, REJEITADA, CANCELADA, NAO_COMPARECEU -> false;
        };
    }

    /** Reserva já iniciada: não pode ser cancelada nem apagada (RF-15). */
    public boolean started() {
        return this == EM_USO || this == CONCLUIDA;
    }

    /** Estados que devolvem o horário à agenda. */
    public boolean releasesSchedule() {
        return this == REJEITADA || this == CANCELADA || this == NAO_COMPARECEU;
    }
}
