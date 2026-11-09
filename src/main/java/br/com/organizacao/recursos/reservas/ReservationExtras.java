package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Liga a agenda (tabela {@code reservations}) ao ciclo de vida auditado (RF-10, RF-14) e as
 * notificacoes (RF-11): toda reserva agendada nasce {@code SOLICITADA}; recursos nao restritos
 * sao aprovados automaticamente pelo sistema e os restritos aguardam o Responsavel (RF-07).
 */
@Service
public class ReservationExtras {
    static final String SYSTEM_ACTOR = "sistema";

    private final ReservationSchedulingService schedulingService;
    private final ReservationAuditService auditService;
    private final ReservationCatalog catalog;
    private final ResourceStore resourceStore;
    private final NotificationService notificationService;
    private final Clock clock;

    public ReservationExtras(
            ReservationSchedulingService schedulingService,
            ReservationAuditService auditService,
            ReservationCatalog catalog,
            ResourceStore resourceStore,
            NotificationService notificationService,
            Clock clock) {
        this.schedulingService = schedulingService;
        this.auditService = auditService;
        this.catalog = catalog;
        this.resourceStore = resourceStore;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    public void onCreated(Long numericId, String resourceId, String requesterId) {
        String id = String.valueOf(numericId);
        auditService.createReservation(id, requesterId, clock, "Solicitação criada");
        catalog.createRestrictedReservation(id, requesterId);
        boolean restricted = resourceStore.findById(resourceId).map(Resource::restricted).orElse(false);
        if (!restricted) {
            ReservationTransition approval = auditService.changeState(id, ReservationStatus.APROVADA,
                    SYSTEM_ACTOR, clock, "Aprovação automática: recurso não restrito");
            catalog.findById(id).applyTransition(approval);
        }
        notificationService.notify(NotificationEvent.CRIACAO, id, requesterId,
                "Reserva " + id + " criada para " + resourceId
                        + (restricted ? " e aguarda aprovação do Responsável." : "."));
    }

    public void onUpdated(String id, String actorId) {
        notificationService.notify(NotificationEvent.ALTERACAO, id, recipient(id, actorId),
                "Reserva " + id + " alterada por " + actorId + ".");
    }

    public void onCancelled(String id, String actorId) {
        String recipient = recipient(id, actorId);
        parseId(id).ifPresent(schedulingService::cancel);
        notificationService.notify(NotificationEvent.CANCELAMENTO, id, recipient,
                "Reserva " + id + " cancelada por " + actorId + ".");
    }

    /** Reconstroi o estado em memoria a partir da trilha de auditoria (ex.: apos reiniciar a aplicacao). */
    public Reservation rehydrate(String id) {
        List<ReservationTransition> history = auditService.history(id);
        if (history.isEmpty()) {
            return null;
        }
        String requester = history.getFirst().actorId();
        // Lanca 404 se a reserva foi cancelada (excluida logicamente).
        auditService.assertCanOperate(id, requester, false);
        Reservation reservation = Reservation.requestForRestrictedResource(id, requester);
        history.stream().skip(1).forEach(reservation::applyTransition);
        return catalog.register(reservation);
    }

    public List<ReservationSummary> list(String requesterId) {
        return schedulingService.list(requesterId).stream()
                .map(summary -> {
                    if (summary.lifecycleStatus() != null || "CANCELADA".equals(summary.schedulingStatus())) {
                        return summary;
                    }
                    Reservation reservation = catalog.findById(summary.id());
                    return reservation == null ? summary
                            : summary.withLifecycleStatus(reservation.status().name());
                })
                .toList();
    }

    private String recipient(String id, String fallback) {
        return parseId(id).flatMap(schedulingService::find).map(ReservationSummary::requesterId)
                .orElse(fallback);
    }

    private java.util.Optional<Long> parseId(String id) {
        try {
            return java.util.Optional.of(Long.parseLong(id));
        } catch (NumberFormatException exception) {
            return java.util.Optional.empty();
        }
    }
}
