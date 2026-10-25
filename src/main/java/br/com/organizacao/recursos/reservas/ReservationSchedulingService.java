package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class ReservationSchedulingService {
    private final ReservationCatalog reservationCatalog;
    private final MaintenanceCatalog maintenanceCatalog;
    private final Clock clock;

    public ReservationSchedulingService(
            ReservationCatalog reservationCatalog,
            MaintenanceCatalog maintenanceCatalog,
            Clock clock) {
        this.reservationCatalog = reservationCatalog;
        this.maintenanceCatalog = maintenanceCatalog;
        this.clock = clock;
    }

    public boolean schedule(CreateReservationRequest request, String requesterId) {
        ReservationPeriod period = new ReservationPeriod(request.startsAt(), request.endsAt());
        if (!isAvailable(request.resourceId(), period)) {
            return false;
        }

        reservationCatalog.schedule(request.resourceId(), requesterId, period);
        return true;
    }

    public boolean isAvailable(String resourceId, ReservationPeriod period) {
        MaintenancePeriod maintenance = maintenanceCatalog.findByResourceId(resourceId);
        return maintenance == null || new MaintenanceRule(clock).isReservable(period, maintenance);
    }
}
