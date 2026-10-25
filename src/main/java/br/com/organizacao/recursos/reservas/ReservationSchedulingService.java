package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import org.springframework.stereotype.Service;

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

    public boolean schedule(CreateReservationRequest request, String requesterId) {
        ReservationPeriod period = new ReservationPeriod(request.startsAt(), request.endsAt());
        String professorId = request.professorId() == null ? requesterId : request.professorId();
        MaintenancePeriod maintenance = maintenanceCatalog.findByResourceId(request.resourceId());
        if (maintenance != null && !new MaintenanceRule(clock).isReservable(period, maintenance)) {
            return false;
        }

        if (!reservationStore.schedule(request.resourceId(), professorId, requesterId, period)) {
            return false;
        }
        reservationCatalog.schedule(request.resourceId(), requesterId, period);
        return true;
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
