package br.com.organizacao.recursos.reservas;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!postgres")
public class InMemoryReservationStore implements ReservationStore {
    @Override
    public boolean schedule(String roomId, String professorId, String requesterId, ReservationPeriod period) {
        return true;
    }

    @Override
    public boolean isAvailable(String roomId, String professorId, ReservationPeriod period) {
        return true;
    }
}
