package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres")
class ProductCliIT {
    private static final String ADMIN_LOGIN = "cli-admin";
    private static final String ADMIN_PASSWORD = "cli-admin-password";
    private static final String SOLICITANT_LOGIN = "cli-teacher";
    private static final String SOLICITANT_PASSWORD = "cli-teacher-password";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("resource_reservations")
            .withUsername("test")
            .withPassword("test");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductUserService userService;

    @Autowired
    private ResourceStore resourceStore;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("RECURSOS_JWT_SECRET", () -> "cli-test-secret-with-at-least-32-bytes-long");
        registry.add("BOOTSTRAP_ADMIN_LOGIN", () -> ADMIN_LOGIN);
        registry.add("BOOTSTRAP_ADMIN_PASSWORD", () -> ADMIN_PASSWORD);
    }

    @BeforeEach
    void prepareApiAndFixtures() {
        jdbcTemplate.update("DELETE FROM reservations");
        jdbcTemplate.update("DELETE FROM reservation_conflict_attempts");
        jdbcTemplate.update("DELETE FROM resources");
        jdbcTemplate.update("DELETE FROM product_user_roles WHERE login <> ?", ADMIN_LOGIN);
        jdbcTemplate.update("DELETE FROM product_users WHERE login <> ?", ADMIN_LOGIN);
        userService.bootstrapAdministrator(ADMIN_LOGIN, ADMIN_PASSWORD);
        userService.createUser(SOLICITANT_LOGIN, SOLICITANT_PASSWORD, ProductUserRole.SOLICITANTE);
        resourceStore.create(new Resource("room-cli", 12));
        jdbcTemplate.update("""
                INSERT INTO reservations (room_id, professor_id, requester_id, starts_at, ends_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                "room-other",
                "another-professor",
                "another-requester",
                Timestamp.from(Instant.parse("2026-11-12T10:00:00Z")),
                Timestamp.from(Instant.parse("2026-11-12T11:00:00Z")));
    }

    @Test
    void packagedJarExecutesResourceAndReservationCommandsOverRealHttp() throws Exception {
        Result resources = runCli(
                "resources", "list", "--user", SOLICITANT_LOGIN,
                SOLICITANT_PASSWORD + "\n");
        assertSuccess(resources);
        assertTrue(resources.output().contains("room-cli\t12"));
        assertFalse(resources.output().contains(SOLICITANT_PASSWORD));

        Result availability = runCli(
                "resources", "availability",
                "--user", SOLICITANT_LOGIN,
                "--resource-id", "room-cli",
                "--starts-at", "2026-11-13T10:00:00Z",
                "--ends-at", "2026-11-13T11:00:00Z",
                SOLICITANT_PASSWORD + "\n");
        assertSuccess(availability);
        assertTrue(availability.output().contains("Disponível"));

        Result created = runCli(
                "reservations", "create",
                "--user", SOLICITANT_LOGIN,
                "--resource-id", "room-cli",
                "--starts-at", "2026-11-13T10:00:00Z",
                "--ends-at", "2026-11-13T11:00:00Z",
                SOLICITANT_PASSWORD + "\n");
        assertSuccess(created);
        assertTrue(created.output().contains("Reserva criada:"));

        Result conflict = runCli(
                "reservations", "create",
                "--user", SOLICITANT_LOGIN,
                "--resource-id", "room-cli",
                "--starts-at", "2026-11-13T10:00:00Z",
                "--ends-at", "2026-11-13T11:00:00Z",
                SOLICITANT_PASSWORD + "\n");
        assertEquals(2, conflict.exitCode());
        assertTrue(conflict.output().contains("Conflito:"));

        Result listed = runCli(
                "reservations", "list", "--user", SOLICITANT_LOGIN,
                SOLICITANT_PASSWORD + "\n");
        assertSuccess(listed);
        assertTrue(listed.output().contains("room-cli"));
        assertFalse(listed.output().contains("room-other"));
        assertFalse(listed.output().contains(SOLICITANT_PASSWORD));
    }

    @Test
    void packagedJarCreatesUsersWithTwoStdinPasswordsAndRejectsProfessorOverride() throws Exception {
        Result created = runCli(
                "users", "create",
                "--user", ADMIN_LOGIN,
                "--login", "cli-created-user",
                "--role", "RESPONSAVEL",
                ADMIN_PASSWORD + "\nnew-user-password\n");
        assertSuccess(created);
        assertTrue(created.output().contains("Usuário criado: cli-created-user (RESPONSAVEL)"));
        assertFalse(created.output().contains(ADMIN_PASSWORD));
        assertFalse(created.output().contains("new-user-password"));
        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM product_users WHERE login = ?", String.class, "cli-created-user");
        assertTrue(passwordEncoder.matches("new-user-password", hash));

        Result override = runCli(
                "reservations", "create",
                "--user", SOLICITANT_LOGIN,
                "--resource-id", "room-cli",
                "--starts-at", "2026-11-14T10:00:00Z",
                "--ends-at", "2026-11-14T11:00:00Z",
                "--professor-id", "another-professor",
                "");
        assertEquals(2, override.exitCode());
        assertTrue(override.output().contains("Opção não suportada: --professor-id"));
    }

    @Test
    void packagedJarProvidesSafeValidationAndAuthenticationErrors() throws Exception {
        Result invalidPeriod = runCli(
                "resources", "availability",
                "--user", SOLICITANT_LOGIN,
                "--resource-id", "room-cli",
                "--starts-at", "2026-11-15T10:00:00Z",
                "--ends-at", "2026-11-15T10:00:00Z",
                "");
        assertEquals(2, invalidPeriod.exitCode());
        assertTrue(invalidPeriod.output().contains("--ends-at deve ser posterior"));

        Result rejectedLogin = runCli(
                "resources", "list", "--user", SOLICITANT_LOGIN,
                "incorrect-password\n");
        assertEquals(2, rejectedLogin.exitCode());
        assertTrue(rejectedLogin.output().contains("Autenticação recusada"));
        assertFalse(rejectedLogin.output().contains("incorrect-password"));
    }

    private Result runCli(String... argumentsAndInput) throws Exception {
        int argumentCount = argumentsAndInput.length - 1;
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-jar");
        command.add(packagedJar().toString());
        command.add("cli");
        command.addAll(List.of(argumentsAndInput).subList(0, argumentCount));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().put("RESOURCE_API_URL", "http://127.0.0.1:" + port);
        Process process = builder.start();
        try (var stdin = process.getOutputStream()) {
            stdin.write(argumentsAndInput[argumentCount].getBytes(StandardCharsets.UTF_8));
        }
        if (!process.waitFor(45, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("CLI process timed out");
        }
        return new Result(process.exitValue(), new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private static Path packagedJar() throws IOException {
        try (Stream<Path> files = Files.list(Path.of("target"))) {
            return files.filter(path -> path.getFileName().toString().startsWith("organizacao-de-recursos-qa-"))
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(path -> !path.getFileName().toString().endsWith(".jar.original"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Packaged application JAR not found"));
        }
    }

    private void assertSuccess(Result result) {
        assertEquals(0, result.exitCode());
    }

    private record Result(int exitCode, String output) {
    }
}