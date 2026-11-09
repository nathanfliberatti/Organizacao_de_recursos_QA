package br.com.organizacao.recursos.reservas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Fluxo completo contra PostgreSQL real, incluindo a protecao de sobreposicao da agenda. */
@Testcontainers
@SpringBootTest(properties = "app.demo-data.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
class ApplicationFlowIT extends AbstractApplicationFlowTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("resource_reservations")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Test
    void overlappingReservationForSameResourceIsRejectedWithConflict() throws Exception {
        String admin = login("admin");
        String solicitante = login("solicitante");
        String resource = "sala-" + suffix();
        createResource(admin, resource, false);
        createReservation(solicitante, resource, "2027-06-01T10:00:00Z", "2027-06-01T11:00:00Z");

        mockMvc.perform(as(post("/api/reservations").content("{\"resourceId\":\"" + resource
                        + "\",\"professorId\":\"outro-prof\",\"startsAt\":\"2027-06-01T10:30:00Z\","
                        + "\"endsAt\":\"2027-06-01T11:30:00Z\"}"), solicitante))
                .andExpect(status().isConflict());
        // Periodos que apenas encostam nao conflitam.
        createReservation(solicitante, resource, "2027-06-01T11:00:00Z", "2027-06-01T12:00:00Z");
    }

    @Test
    void concurrentRequestsForSameSlotConfirmOnlyOneReservation() throws Exception {
        String admin = login("admin");
        String solicitante = login("solicitante");
        String resource = "sala-" + suffix();
        createResource(admin, resource, false);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<CompletableFuture<Integer>> calls = java.util.stream.IntStream.range(0, 8)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return mockMvc.perform(as(post("/api/reservations").content("{\"resourceId\":\"" + resource
                                            + "\",\"professorId\":\"prof-" + i + "\",\"startsAt\":\"2027-07-01T10:00:00Z\","
                                            + "\"endsAt\":\"2027-07-01T11:00:00Z\"}"), solicitante))
                                    .andReturn().getResponse().getStatus();
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    }, executor))
                    .toList();
            long created = calls.stream().map(CompletableFuture::join).filter(code -> code == 201).count();
            assertEquals(1, created);
        } finally {
            executor.shutdownNow();
        }
    }
}
