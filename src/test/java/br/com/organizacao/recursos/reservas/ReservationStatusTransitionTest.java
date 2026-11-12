package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/** RF-14: matriz completa de transições (todos os 49 pares de estados). */
class ReservationStatusTransitionTest {
    private record Pair(ReservationStatus from, ReservationStatus to) {
    }

    /** Fonte da verdade do teste, escrita à mão e independente da implementação. */
    private static final Set<Pair> ALLOWED = Set.of(
            new Pair(ReservationStatus.SOLICITADA, ReservationStatus.APROVADA),
            new Pair(ReservationStatus.SOLICITADA, ReservationStatus.REJEITADA),
            new Pair(ReservationStatus.SOLICITADA, ReservationStatus.CANCELADA),
            new Pair(ReservationStatus.APROVADA, ReservationStatus.EM_USO),
            new Pair(ReservationStatus.APROVADA, ReservationStatus.CANCELADA),
            new Pair(ReservationStatus.APROVADA, ReservationStatus.NAO_COMPARECEU),
            new Pair(ReservationStatus.EM_USO, ReservationStatus.CONCLUIDA));

    static Stream<Arguments> everyPair() {
        return Stream.of(ReservationStatus.values()).flatMap(from -> Stream.of(ReservationStatus.values())
                .map(to -> Arguments.of(Named.of(from + " → " + to, new Pair(from, to)))));
    }

    @ParameterizedTest
    @MethodSource("everyPair")
    void onlyTheDocumentedTransitionsAreAllowed(Pair pair) {
        assertEquals(ALLOWED.contains(pair), pair.from().canMoveTo(pair.to()));
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"CONCLUIDA", "REJEITADA", "CANCELADA", "NAO_COMPARECEU"})
    void finalStatesAllowNoTransition(ReservationStatus finalStatus) {
        for (ReservationStatus next : ReservationStatus.values()) {
            assertEquals(false, finalStatus.canMoveTo(next));
        }
    }

    @ParameterizedTest
    @EnumSource(ReservationStatus.class)
    void startedAndReleasingFlagsMatchTheRules(ReservationStatus status) {
        assertEquals(status == ReservationStatus.EM_USO || status == ReservationStatus.CONCLUIDA, status.started());
        assertEquals(status == ReservationStatus.REJEITADA || status == ReservationStatus.CANCELADA
                || status == ReservationStatus.NAO_COMPARECEU, status.releasesSchedule());
    }
}
