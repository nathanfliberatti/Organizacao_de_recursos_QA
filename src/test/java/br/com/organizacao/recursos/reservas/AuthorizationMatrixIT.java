package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = "performance.auth.secret=authorization-matrix-test-secret")
@AutoConfigureMockMvc
@ActiveProfiles({"perf", "postgres"})
class AuthorizationMatrixIT {
    private static final String RESERVATION_ID = "matrix-reservation";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-02T22:00:00Z"), ZoneOffset.UTC);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("resource_reservations")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ReservationCatalog reservationCatalog;
    @Autowired
    private ReservationAuditService reservationAuditService;
    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @BeforeEach
    void clearFixtures() {
        jdbcTemplate.update("DELETE FROM reservation_audit");
        jdbcTemplate.update("DELETE FROM reservation_lifecycle");
        jdbcTemplate.update("DELETE FROM reservation_conflict_attempts");
        jdbcTemplate.update("DELETE FROM reservations");
        reservationCatalog.clear();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("authorizationMatrix")
    void apiMatchesAuthorizationMatrix(MatrixCase testCase) throws Exception {
        if (testCase.fixture() != Fixture.NONE) {
            seedReservation(testCase.fixture());
        }

        MockHttpServletRequestBuilder request = request(testCase);
        if (testCase.role() != Role.ANONYMOUS) {
            request.header("Authorization", "Bearer " + token(testCase.role(), testCase.subject()));
        }

        mockMvc.perform(request).andExpect(status().is(testCase.expectedStatus()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void invalidInputReturnsSafeProblemDetail(InvalidRequest invalidRequest) throws Exception {
        MvcResult result = mockMvc.perform(invalidRequest.request()
                        .header("Authorization", "Bearer " + token(Role.ADMINISTRADOR, "admin-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest.body()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode problem = objectMapper.readTree(response);
        assertTrue(problem.has("title"));
        assertTrue(problem.has("detail"));
        assertTrue(problem.get("detail").asText().length() > 0);
        org.junit.jupiter.api.Assertions.assertEquals(400, problem.get("status").asInt());
        assertFalse(response.matches("(?is).*\\b(at [\\w.$]+\\([^)]*\\.java:\\d+\\)|select\\s|insert\\s|update\\s|delete\\s|org\\.springframework|org\\.postgresql|SQLException)\\b.*"));
    }

    @Test
    void requesterCannotModifyAnotherRequestersReservation() throws Exception {
        seedReservation(Fixture.SCHEDULED);
        String token = token(Role.SOLICITANTE, "requester-outsider");

        mockMvc.perform(put("/api/reservations/9001")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void requesterCannotCancelAnotherRequestersReservation() throws Exception {
        seedReservation(Fixture.SOLICITADA);
        String token = token(Role.SOLICITANTE, "requester-outsider");

        mockMvc.perform(delete("/api/reservations/{id}", RESERVATION_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    static Stream<Named<MatrixCase>> authorizationMatrix() {
        return Stream.concat(
                Stream.of(
                row("POST /api/auth/login", "POST", "/api/auth/login", Role.ANONYMOUS, 200, Fixture.NONE),
                row("POST /api/auth/login", "POST", "/api/auth/login", Role.SOLICITANTE, 200, Fixture.NONE),
                row("POST /api/auth/login", "POST", "/api/auth/login", Role.RESPONSAVEL, 200, Fixture.NONE),
                row("POST /api/auth/login", "POST", "/api/auth/login", Role.ADMINISTRADOR, 200, Fixture.NONE)),
                Stream.of(
                matrix("GET /api/reservations/restricted", "GET", "/api/reservations/restricted",
                        Fixture.NONE, 401, 200, 200, 200),
                matrix("POST /api/reservations/restricted", "POST", "/api/reservations/restricted",
                        Fixture.NONE, 401, 201, 201, 201),
                matrix("GET /api/reservations/{id}", "GET", "/api/reservations/" + RESERVATION_ID,
                        Fixture.SOLICITADA, 401, 200, 200, 200),
                matrix("GET /api/reservations/{id}/history", "GET",
                        "/api/reservations/" + RESERVATION_ID + "/history",
                        Fixture.SOLICITADA, 401, 403, 200, 200),
                matrix("DELETE /api/reservations/{id}", "DELETE",
                        "/api/reservations/" + RESERVATION_ID, Fixture.SOLICITADA,
                        401, 204, 204, 204),
                matrix("POST /api/reservations/{id}/approval", "POST",
                        "/api/reservations/" + RESERVATION_ID + "/approval",
                        Fixture.SOLICITADA, 401, 403, 200, 403),
                matrix("POST /api/reservations", "POST", "/api/reservations",
                        Fixture.NONE, 401, 201, 201, 201),
                matrix("GET /api/resources/{resourceId}/availability", "GET",
                        "/api/resources/matrix-room/availability"
                                + "?startsAt=2026-11-03T10:00:00Z&endsAt=2026-11-03T11:00:00Z",
                        Fixture.NONE,
                        401, 200, 200, 200),
                matrix("POST /api/reservations/{id}/start", "POST",
                        "/api/reservations/" + RESERVATION_ID + "/start",
                        Fixture.APROVADA, 401, 200, 200, 200),
                matrix("POST /api/reservations/{id}/complete", "POST",
                        "/api/reservations/" + RESERVATION_ID + "/complete",
                        Fixture.EM_USO, 401, 200, 200, 200),
                matrix("POST /api/resources/{resourceId}/maintenance", "POST",
                        "/api/resources/matrix-maintenance/maintenance", Fixture.NONE,
                        401, 403, 403, 201),
                matrix("GET /relatorios/utilizacao", "GET",
                        "/relatorios/utilizacao?inicio=2026-11-01T00:00:00Z&fim=2026-11-02T00:00:00Z",
                        Fixture.NONE, 401, 403, 200, 200),
                matrix("PUT /api/reservations/{id}", "PUT",
                        "/api/reservations/9001", Fixture.SCHEDULED,
                        401, 200, 200, 200),
                matrix("POST /api/resources", "POST", "/api/resources",
                        Fixture.NONE, 401, 403, 403, 201))
                .flatMap(stream -> stream));
    }

    static Stream<Named<InvalidRequest>> invalidRequests() {
        return Stream.of(
                Named.of("reservation period ends before it starts",
                        new InvalidRequest(post("/api/reservations"),
                                """
                                {"resourceId":"invalid-period","startsAt":"2026-11-02T12:00:00Z","endsAt":"2026-11-02T11:00:00Z"}
                                """)),
                Named.of("reservation required fields are missing",
                        new InvalidRequest(post("/api/reservations"), """
                                {"resourceId":"missing-period"}
                                """)),
                Named.of("resource capacity is negative",
                        new InvalidRequest(post("/api/resources"), """
                                {"id":"negative-capacity","capacity":-1}
                                """)),
                Named.of("resource capacity is missing",
                        new InvalidRequest(post("/api/resources"), """
                                {"id":"missing-capacity"}
                                """)));
    }

    private static Named<MatrixCase> row(
            String operation, String method, String path, Role role, int expected, Fixture fixture) {
        String subject = role == Role.SOLICITANTE ? "requester-owner" : role.subject();
        return Named.of(operation + " / " + role + " => " + expected,
                new MatrixCase(operation, method, path, role, subject, expected, fixture));
    }

    private static Stream<Named<MatrixCase>> matrix(
            String operation, String method, String path, Fixture fixture,
            int anonymous, int requester, int responsible, int administrator) {
        return Stream.of(
                row(operation, method, path, Role.ANONYMOUS, anonymous, fixture),
                row(operation, method, path, Role.SOLICITANTE, requester, fixture),
                row(operation, method, path, Role.RESPONSAVEL, responsible, fixture),
                row(operation, method, path, Role.ADMINISTRADOR, administrator, fixture));
    }

    private MockHttpServletRequestBuilder request(MatrixCase testCase) {
        MockHttpServletRequestBuilder request = switch (testCase.method()) {
            case "GET" -> get(testCase.path());
            case "POST" -> post(testCase.path());
            case "PUT" -> put(testCase.path());
            case "DELETE" -> delete(testCase.path());
            default -> throw new IllegalArgumentException("Unsupported method in authorization matrix");
        };
        if (testCase.method().equals("POST") || testCase.method().equals("PUT")) {
            request.contentType(MediaType.APPLICATION_JSON).content(body(testCase));
        }
        return request;
    }

    private String body(MatrixCase testCase) {
        if (testCase.path().equals("/api/auth/login")) {
            return """
                    {"username":"perf-user-01","password":"local-only-placeholder-01"}
                    """;
        }
        if (testCase.path().equals("/api/reservations")) {
            return """
                    {"resourceId":"matrix-room","startsAt":"2026-11-03T10:00:00Z","endsAt":"2026-11-03T11:00:00Z"}
                    """;
        }
        if (testCase.path().startsWith("/api/reservations/") && testCase.method().equals("PUT")) {
            return validUpdateBody();
        }
        if (testCase.path().endsWith("/maintenance")) {
            return """
                    {"startsAt":"2026-11-03T10:00:00Z","endsAt":"2026-11-03T11:00:00Z"}
                    """;
        }
        if (testCase.path().equals("/api/resources")) {
            return """
                    {"id":"matrix-resource","capacity":10}
                    """;
        }
        return "{}";
    }

    private String token(Role role, String subject) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(600))
                .claim("roles", role == Role.ANONYMOUS ? List.of() : List.of(role.authority()))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private void seedReservation(Fixture fixture) {
        if (fixture == Fixture.SCHEDULED) {
            jdbcTemplate.update("""
                    INSERT INTO reservations (id, room_id, professor_id, starts_at, ends_at)
                    OVERRIDING SYSTEM VALUE VALUES (?, ?, ?, ?, ?)
                    """,
                    9001,
                    "matrix-room",
                    "requester-owner",
                    java.sql.Timestamp.from(Instant.parse("2026-11-03T10:00:00Z")),
                    java.sql.Timestamp.from(Instant.parse("2026-11-03T11:00:00Z")));
            return;
        }
        reservationCatalog.createRestrictedReservation(RESERVATION_ID, "requester-owner");
        reservationAuditService.createReservation(RESERVATION_ID, "requester-owner", CLOCK, "Solicitação criada");
        if (fixture == Fixture.APROVADA || fixture == Fixture.EM_USO) {
            reservationAuditService.changeState(
                    RESERVATION_ID, ReservationStatus.APROVADA, "responsible-1", CLOCK, "Aprovada");
        }
        if (fixture == Fixture.EM_USO) {
            reservationAuditService.changeState(
                    RESERVATION_ID, ReservationStatus.EM_USO, "requester-owner", CLOCK, "Iniciada");
        }
    }

    private static String validUpdateBody() {
        return """
                {"startsAt":"2026-11-03T10:00:00Z","endsAt":"2026-11-03T11:00:00Z"}
                """;
    }

    private enum Role {
        ANONYMOUS(null, null),
        SOLICITANTE("requester-owner", "SOLICITANTE"),
        RESPONSAVEL("responsible-1", "RESPONSAVEL"),
        ADMINISTRADOR("admin-1", "ADMINISTRADOR");

        private final String subject;
        private final String authority;

        Role(String subject, String authority) {
            this.subject = subject;
            this.authority = authority;
        }

        String subject() {
            return subject;
        }

        String authority() {
            return authority;
        }
    }

    private enum Fixture {
        NONE,
        SCHEDULED,
        SOLICITADA,
        APROVADA,
        EM_USO
    }

    private record MatrixCase(
            String operation, String method, String path, Role role,
            String subject, int expectedStatus, Fixture fixture) {
    }

    private record InvalidRequest(MockHttpServletRequestBuilder request, String body) {
    }
}
