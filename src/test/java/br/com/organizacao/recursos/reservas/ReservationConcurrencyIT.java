package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
class ReservationConcurrencyIT {
    private static final int REQUEST_COUNT = 20;
    private static final Instant STARTS_AT = Instant.parse("2026-10-25T10:00:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-10-25T11:00:00Z");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("resource_reservations")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @RepeatedTest(5)
    void onlyOneConcurrentReservationIsAcceptedForSameRoomAndTime() throws Exception {
        verifyExactlyOneAcceptedAndOneDatabaseRow(
                index -> new CreateReservationRequest("room-shared", STARTS_AT, ENDS_AT,
                        "professor-" + index),
                "room_id",
                "room-shared");
    }

    @RepeatedTest(5)
    void onlyOneConcurrentReservationIsAcceptedForSameProfessorAndTime() throws Exception {
        verifyExactlyOneAcceptedAndOneDatabaseRow(
                index -> new CreateReservationRequest("room-" + index, STARTS_AT, ENDS_AT,
                        "professor-shared"),
                "professor_id",
                "professor-shared");
    }

        @Test
    void roomAvailabilityQueriesPostgresWithoutProfessorFilter() throws Exception {
        jdbcTemplate.update("TRUNCATE TABLE reservations RESTART IDENTITY");
        jdbcTemplate.update(
                "INSERT INTO reservations (room_id, professor_id, starts_at, ends_at) VALUES (?, ?, ?, ?)",
                "room-from-db",
                "professor-from-db",
                Timestamp.from(STARTS_AT),
                Timestamp.from(ENDS_AT));

        mockMvc.perform(get("/api/resources/{resourceId}/availability", "room-from-db")
                        .queryParam("startsAt", "2026-10-25T09:00:00Z")
                        .queryParam("endsAt", "2026-10-25T09:30:00Z")
                        .with(jwt().jwt(token -> token.subject("requester-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        mockMvc.perform(get("/api/resources/{resourceId}/availability", "room-from-db")
                        .queryParam("startsAt", STARTS_AT.toString())
                        .queryParam("endsAt", ENDS_AT.toString())
                        .with(jwt().jwt(token -> token.subject("requester-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    private void verifyExactlyOneAcceptedAndOneDatabaseRow(
            IntFunction<CreateReservationRequest> requestFactory,
            String resourceColumn,
            String resourceId) throws Exception {
        jdbcTemplate.update("TRUNCATE TABLE reservations RESTART IDENTITY");
        CountDownLatch ready = new CountDownLatch(REQUEST_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT);
        List<Future<Integer>> responses = new ArrayList<>();

        try {
            for (int index = 0; index < REQUEST_COUNT; index++) {
                CreateReservationRequest request = requestFactory.apply(index);
                String body = objectMapper.writeValueAsString(request);
                int requestIndex = index;
                responses.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent start latch timed out");
                    }
                    MvcResult result = mockMvc.perform(post("/api/reservations")
                                    .with(jwt().jwt(token -> token.subject("requester-" + requestIndex))
                                            .authorities(new SimpleGrantedAuthority("ROLE_SOLICITANTE")))
                                    .with(csrf())
                                    .contentType("application/json")
                                    .content(body))
                            .andReturn();
                    return result.getResponse().getStatus();
                }));
            }

            assertTrue(ready.await(30, TimeUnit.SECONDS), "All request workers should be ready before release");
            start.countDown();
            List<Integer> statusCodes = new ArrayList<>(REQUEST_COUNT);
            for (Future<Integer> response : responses) {
                statusCodes.add(response.get(60, TimeUnit.SECONDS));
            }

            long accepted = statusCodes.stream().filter(statusCode -> statusCode == 201).count();
            long conflicts = statusCodes.stream().filter(statusCode -> statusCode == 409).count();
            long persistedRows = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM reservations WHERE " + resourceColumn
                            + " = ? AND starts_at = ? AND ends_at = ?",
                    Long.class,
                    resourceId,
                    Timestamp.from(STARTS_AT),
                    Timestamp.from(ENDS_AT));

            assertAll(
                    () -> assertEquals(1, accepted, "Exactly one request should be accepted with HTTP 201"),
                    () -> assertEquals(REQUEST_COUNT - 1, conflicts,
                            "All remaining requests should be rejected with HTTP 409"),
                    () -> assertEquals(1, persistedRows,
                            "PostgreSQL should contain exactly one row for the conflicting interval"));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "Request workers should terminate");
        }
    }
}
