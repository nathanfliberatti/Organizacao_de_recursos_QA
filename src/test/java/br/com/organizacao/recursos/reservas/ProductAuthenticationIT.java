package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
class ProductAuthenticationIT {
    private static final String ADMIN_LOGIN = "it-admin";
    private static final String ADMIN_PASSWORD = "test-password-only";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("resource_reservations")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private ProductUserService userService;

    @Autowired
    private ResourceStore resourceStore;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("RECURSOS_JWT_SECRET", () -> "product-auth-test-secret-with-at-least-32-bytes");
        registry.add("BOOTSTRAP_ADMIN_LOGIN", () -> ADMIN_LOGIN);
        registry.add("BOOTSTRAP_ADMIN_PASSWORD", () -> ADMIN_PASSWORD);
    }

    @BeforeEach
    void clearProductFixtures() {
        jdbcTemplate.update("DELETE FROM reservations");
        jdbcTemplate.update("DELETE FROM reservation_conflict_attempts");
        jdbcTemplate.update("DELETE FROM resources");
        jdbcTemplate.update("DELETE FROM product_user_roles WHERE login <> ?", ADMIN_LOGIN);
        jdbcTemplate.update("DELETE FROM product_users WHERE login <> ?", ADMIN_LOGIN);
    }

    @AfterEach
    void ensureBootstrapAdministratorExists() {
        userService.bootstrapAdministrator(ADMIN_LOGIN, ADMIN_PASSWORD);
    }

    @Test
    void bootstrappedAdministratorCanObtainBearerToken() throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"it-admin","password":"test-password-only"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString())
            .path("accessToken").asText();
        var claims = jwtDecoder.decode(token);
        assertEquals(ADMIN_LOGIN, claims.getSubject());
        assertTrue(Duration.between(claims.getIssuedAt(), claims.getExpiresAt())
            .compareTo(Duration.ofMinutes(15)) <= 0);
        }

        @Test
        void bootstrapStoresBcryptAndDoesNotResetAnExistingAdministrator() {
        String originalHash = jdbcTemplate.queryForObject(
            "SELECT password_hash FROM product_users WHERE login = ?", String.class, ADMIN_LOGIN);
        assertTrue(passwordEncoder.matches(ADMIN_PASSWORD, originalHash));

        userService.bootstrapAdministrator(ADMIN_LOGIN, "different-password");

        assertEquals(originalHash, jdbcTemplate.queryForObject(
            "SELECT password_hash FROM product_users WHERE login = ?", String.class, ADMIN_LOGIN));
        }

        @Test
        void bootstrapFailsClosedWhenNoAdministratorAndCredentialsAreMissing() {
        jdbcTemplate.update("DELETE FROM product_user_roles WHERE login = ?", ADMIN_LOGIN);
        jdbcTemplate.update("DELETE FROM product_users WHERE login = ?", ADMIN_LOGIN);

        assertThrows(IllegalStateException.class, () -> userService.bootstrapAdministrator(null, null));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_users", Integer.class));
        }

        @Test
        void invalidCredentialsAndInactiveUsersCannotAuthenticate() {
        userService.createUser("inactive-user", "inactive-password", ProductUserRole.SOLICITANTE);
        jdbcTemplate.update("UPDATE product_users SET active = FALSE WHERE login = ?", "inactive-user");

        assertLoginUnauthorized(ADMIN_LOGIN, "wrong-password");
        assertLoginUnauthorized("inactive-user", "inactive-password");
        }

        @Test
        void userStatusAndCurrentRolesAreRevalidatedForEachBearerRequest() throws Exception {
        userService.createUser("teacher-1", "teacher-password", ProductUserRole.SOLICITANTE);
        String token = loginToken("teacher-1", "teacher-password");

        jdbcTemplate.update("DELETE FROM product_user_roles WHERE login = ?", "teacher-1");
        jdbcTemplate.update("INSERT INTO product_user_roles (login, role) VALUES (?, ?)",
            "teacher-1", ProductUserRole.ADMINISTRADOR.name());
        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"login":"created-after-role-change","password":"initial-password","role":"RESPONSAVEL"}
                    """))
            .andExpect(status().isCreated());

        jdbcTemplate.update("UPDATE product_users SET active = FALSE WHERE login = ?", "teacher-1");
        mockMvc.perform(get("/api/resources").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
        }

        @Test
        void userCreationRequiresAdministratorOnTheServer() throws Exception {
        String body = """
            {"login":"blocked-user","password":"initial-password","role":"SOLICITANTE"}
            """;
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());

        userService.createUser("teacher-1", "teacher-password", ProductUserRole.SOLICITANTE);
        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + loginToken("teacher-1", "teacher-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
        assertEquals(0, jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM product_users WHERE login = ?", Integer.class, "blocked-user"));
        }

        @Test
        void administratorCanCreateBcryptUserWithAllowedRole() throws Exception {
        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + loginToken(ADMIN_LOGIN, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"login":"new-teacher","password":"initial-password","role":"RESPONSAVEL"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value("new-teacher"))
            .andExpect(jsonPath("$.role").value("RESPONSAVEL"))
            .andExpect(jsonPath("$.active").value(true));

        String hash = jdbcTemplate.queryForObject(
            "SELECT password_hash FROM product_users WHERE login = ?", String.class, "new-teacher");
        assertTrue(passwordEncoder.matches("initial-password", hash));
        }

        @Test
        void resourceCatalogReturnsOnlyPersistedIdAndCapacity() throws Exception {
        resourceStore.create(new Resource("room-api", 24));
        mockMvc.perform(get("/api/resources")
                .header("Authorization", "Bearer " + loginToken(ADMIN_LOGIN, ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("room-api"))
            .andExpect(jsonPath("$[0].capacity").value(24))
            .andExpect(jsonPath("$[0].type").doesNotExist());
        }

        @Test
        void reservationCreateRejectsAlternativeProfessorWithoutPersisting() throws Exception {
        resourceStore.create(new Resource("room-owned", 8));
        userService.createUser("teacher-1", "teacher-password", ProductUserRole.SOLICITANTE);
        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + loginToken("teacher-1", "teacher-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"resourceId":"room-owned","professorId":"other-professor","startsAt":"2026-11-10T10:00:00Z","endsAt":"2026-11-10T11:00:00Z"}
                    """))
            .andExpect(status().isBadRequest());
        assertEquals(0, jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM reservations WHERE room_id = ?", Integer.class, "room-owned"));
        }

        @Test
        void ownReservationListIsEnforcedByApiForRequestersAndAdministrators() throws Exception {
        userService.createUser("teacher-1", "teacher-password", ProductUserRole.SOLICITANTE);
        insertReservation("admin-room", ADMIN_LOGIN);
        insertReservation("teacher-room", "teacher-1");
        insertReservation("third-party-room", "someone-else");

        mockMvc.perform(get("/api/reservations")
                .header("Authorization", "Bearer " + loginToken(ADMIN_LOGIN, ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].resourceId").value("admin-room"));
        mockMvc.perform(get("/api/reservations")
                .header("Authorization", "Bearer " + loginToken("teacher-1", "teacher-password")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].resourceId").value("teacher-room"));
        }

        @Test
        void availabilityRejectsEmptyOrReversedPeriods() throws Exception {
        mockMvc.perform(get("/api/resources/room-api/availability")
                .header("Authorization", "Bearer " + loginToken(ADMIN_LOGIN, ADMIN_PASSWORD))
                .queryParam("startsAt", "2026-11-10T10:00:00Z")
                .queryParam("endsAt", "2026-11-10T10:00:00Z"))
            .andExpect(status().isBadRequest());
        }

        private String loginToken(String login, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.createObjectNode().put("login", login).put("password", password)
                    .toString()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.path("accessToken").asText();
        }

        private void assertLoginUnauthorized(String login, String password) {
        try {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.createObjectNode().put("login", login).put("password", password)
                        .toString()))
                .andExpect(status().isUnauthorized());
        } catch (Exception exception) {
            throw new AssertionError("Login request should complete", exception);
        }
        }

        private void insertReservation(String resourceId, String requesterId) {
        jdbcTemplate.update("""
            INSERT INTO reservations (room_id, professor_id, requester_id, starts_at, ends_at)
            VALUES (?, ?, ?, ?, ?)
            """,
            resourceId,
            requesterId,
            requesterId,
            java.sql.Timestamp.from(Instant.parse("2026-11-10T10:00:00Z")),
            java.sql.Timestamp.from(Instant.parse("2026-11-10T11:00:00Z")));
    }
}