package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import java.util.Optional;
import java.util.OptionalLong;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservationSchedulingService {
    private final ReservationCatalog reservationCatalog;
    private final ReservationStore reservationStore;
    private final MaintenanceCatalog maintenanceCatalog;
    private final Clock clock;

    public ReservationSchedulingService(
            ReservationCatalog reservationCatalog,
            ReservationStore reservationStore,
            MaintenanceCatalog maintenanceCatalog,
            Clock clock) {
        this.reservationCatalog = reservationCatalog;
        this.reservationStore = reservationStore;
        this.maintenanceCatalog = maintenanceCatalog;
        this.clock = clock;
    }

    public ReservationAttemptResponse schedule(CreateReservationRequest request, String requesterId) {
        ReservationPeriod period = new ReservationPeriod(request.startsAt(), request.endsAt());
        String professorId = request.professorId() == null ? requesterId : request.professorId();
        MaintenancePeriod maintenance = maintenanceCatalog.findByResourceId(request.resourceId());
        if (maintenance != null && !new MaintenanceRule(clock).isReservable(period, maintenance)) {
            return new ReservationAttemptResponse(false);
        }

        OptionalLong reservationId = reservationStore.scheduleWithId(
                request.resourceId(), professorId, requesterId, period);
        if (reservationId.isEmpty()) {
            reservationStore.recordConflictAttempt(request.resourceId(), professorId, period);
            return new ReservationAttemptResponse(false);
        }
        reservationCatalog.schedule(request.resourceId(), requesterId, period);
        return new ReservationAttemptResponse(true, reservationId.getAsLong());
    }

    @Transactional
    public void update(long reservationId, String requesterId, boolean canModifyAny, ReservationPeriod period) {
        if (period.startsAt() == null || period.endsAt() == null || !period.startsAt().isBefore(period.endsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endsAt must be later than startsAt");
        }
        Optional<String> owner = reservationStore.requesterId(reservationId);
        if (owner.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found");
        }
        if (!canModifyAny && !owner.get().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to modify this reservation");
        }
        reservationStore.update(reservationId, period);
    }

    public boolean isAvailable(String resourceId, ReservationPeriod period) {
        return isAvailable(resourceId, null, period);
    }

    public boolean isAvailable(String resourceId, String professorId, ReservationPeriod period) {
        MaintenancePeriod maintenance = maintenanceCatalog.findByResourceId(resourceId);
        return (maintenance == null || new MaintenanceRule(clock).isReservable(period, maintenance))
                && reservationStore.isAvailable(resourceId, professorId, period);
    }
}
