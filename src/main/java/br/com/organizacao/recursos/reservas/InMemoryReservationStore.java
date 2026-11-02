package br.com.organizacao.recursos.reservas;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!postgres")
public class InMemoryReservationStore implements ReservationStore {
    private final List<RejectedAttempt> rejectedAttempts = new CopyOnWriteArrayList<>();

    @Override
    public boolean schedule(String roomId, String professorId, String requesterId, ReservationPeriod period) {
        return true;
    }

    @Override
    public void recordConflictAttempt(String roomId, String professorId, ReservationPeriod period) {
        rejectedAttempts.add(new RejectedAttempt(roomId, professorId, period));
    }

    @Override
    public boolean isAvailable(String roomId, String professorId, ReservationPeriod period) {
        return true;
    }

    private record RejectedAttempt(String roomId, String professorId, ReservationPeriod period) {
    }
}
