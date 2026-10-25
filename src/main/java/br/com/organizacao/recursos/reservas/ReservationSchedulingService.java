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
        MaintenancePeriod maintenance = maintenanceCatalog.findByResourceId(request.resourceId());
        ReservationPeriod period = new ReservationPeriod(request.startsAt(), request.endsAt());
        if (maintenance != null && !new MaintenanceRule(clock).isReservable(period, maintenance)) {
            return false;
        }

        reservationCatalog.schedule(request.resourceId(), requesterId, period);
        return true;
    }
}
