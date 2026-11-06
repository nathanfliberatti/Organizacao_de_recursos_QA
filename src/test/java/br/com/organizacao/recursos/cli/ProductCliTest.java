package br.com.organizacao.recursos.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProductCliTest {
    private static final String PASSWORD = "unit-test-password";
    private static final String TOKEN = "unit-test-token";

    private final Map<String, Response> responses = new ConcurrentHashMap<>();
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void resourcesListAuthenticatesAndDisplaysOnlyCatalogFields() {
        respond("GET", "/api/resources", 200, "[{\"id\":\"room-1\",\"capacity\":24}]");

        Result result = run("resources", "list", "--user", "teacher", PASSWORD + "\n");

        assertSuccess(result);
        assertTrue(result.output().contains("ID\tCAPACIDADE"));
        assertTrue(result.output().contains("room-1\t24"));
        assertEquals("Bearer " + TOKEN, request("GET", "/api/resources").authorization());
        assertTrue(request("POST", "/api/auth/login").body().contains(PASSWORD));
    }

    @Test
    void availabilitySupportsEncodedInputsAndUnavailableResults() {
        respond("GET", "/api/resources/room%201/availability?startsAt=2026-11-10T10%3A00%3A00Z"
                + "&endsAt=2026-11-10T11%3A00%3A00Z&professorId=teacher%20one",
                200, "{\"available\":false}");

        Result result = run("resources", "availability",
                "--user", "teacher", "--resource-id", "room 1",
                "--starts-at", "2026-11-10T10:00:00Z", "--ends-at", "2026-11-10T11:00:00Z",
                "--professor-id", "teacher one", PASSWORD + "\n");

        assertSuccess(result);
        assertTrue(result.output().contains("Indisponível"));
    }

    @Test
    void createReservationSendsNoAlternativeProfessorIdentity() {
        respond("POST", "/api/reservations", 201, "{\"accepted\":true,\"reservationId\":42}");

        Result result = run("reservations", "create", "--user", "teacher",
                "--resource-id", "room-1", "--starts-at", "2026-11-10T10:00:00Z",
                "--ends-at", "2026-11-10T11:00:00Z", PASSWORD + "\n");

        assertSuccess(result);
        assertTrue(result.output().contains("Reserva criada: 42"));
        assertFalse(request("POST", "/api/reservations").body().contains("professorId"));
    }

    @Test
    void reservationListDisplaysReturnedReservations() {
        respond("GET", "/api/reservations", 200,
                "[{\"id\":42,\"resourceId\":\"room-1\",\"startsAt\":\"start\",\"endsAt\":\"end\"}]");

        Result result = run("reservations", "list", "--user", "teacher", PASSWORD + "\n");

        assertSuccess(result);
        assertTrue(result.output().contains("42\troom-1\tstart\tend"));
    }

    @Test
    void createUserReadsBothPasswordsAndDoesNotPrintEither() {
        respond("POST", "/api/users", 201, "{\"login\":\"new-user\",\"role\":\"RESPONSAVEL\"}");

        Result result = run("users", "create", "--user", "admin", "--login", "new-user",
                "--role", "RESPONSAVEL", PASSWORD + "\nnew-user-password\n");

        assertSuccess(result);
        assertTrue(result.output().contains("Usuário criado: new-user (RESPONSAVEL)"));
        assertFalse(result.output().contains(PASSWORD));
        assertFalse(result.output().contains("new-user-password"));
        assertTrue(request("POST", "/api/users").body().contains("new-user-password"));
    }

    @Test
    void authenticationAndApiResponsesBecomeSafeMessages() {
        respond("POST", "/api/auth/login", 401, "{}");
        Result login = run("resources", "list", "--user", "teacher", "bad-password\n");
        assertFailure(login, "recusada");
        assertFalse(login.output().contains("bad-password"));

        respond("GET", "/api/resources", 403, "{}");
        assertFailure(run("resources", "list", "--user", "teacher", PASSWORD + "\n"),
                "Permissão insuficiente");

        respond("GET", "/api/resources", 409, "{}");
        assertFailure(run("resources", "list", "--user", "teacher", PASSWORD + "\n"), "Conflito:");

        respond("GET", "/api/resources", 422, "{}");
        assertFailure(run("resources", "list", "--user", "teacher", PASSWORD + "\n"),
                "Requisição inválida");

        respond("GET", "/api/resources", 503, "{}");
        assertFailure(run("resources", "list", "--user", "teacher", PASSWORD + "\n"), "HTTP 503");
    }

    @Test
    void invalidResponsesAndIntervalsAreReportedWithoutStackTraces() {
        respond("POST", "/api/auth/login", 200, "{}");
        assertFailure(run("resources", "list", "--user", "teacher", PASSWORD + "\n"),
                "resposta de autenticação inválida");

        respond("POST", "/api/auth/login", 200, "{\"accessToken\":\"token\"}");
        respond("GET", "/api/resources", 200, "not-json");
        assertFailure(run("resources", "list", "--user", "teacher", PASSWORD + "\n"),
                "resposta inválida");

        respond("GET", "/api/resources/room/availability?startsAt=2026-11-10T10%3A00%3A00Z"
                + "&endsAt=2026-11-10T11%3A00%3A00Z", 400, "{}");
        Result invalidPeriod = run("resources", "availability", "--user", "teacher",
                "--resource-id", "room", "--starts-at", "2026-11-10T10:00:00Z",
                "--ends-at", "2026-11-10T11:00:00Z", PASSWORD + "\n");
        assertFailure(invalidPeriod, "Intervalo inválido");

        respond("POST", "/api/reservations", 409, "{}");
        assertFailure(run("reservations", "create", "--user", "teacher", "--resource-id", "room",
                "--starts-at", "2026-11-10T10:00:00Z", "--ends-at", "2026-11-10T11:00:00Z",
                PASSWORD + "\n"), "Conflito: o recurso ou professor");
    }

    @ParameterizedTest
    @CsvSource({
            "uso, cli <resources|reservations|users>",
            "invalido, Comando inválido.",
            "sem-user, Informe --user.",
            "par-errado, Cada opção deve usar",
            "opcao-nao-suportada, Opção não suportada",
            "opcao-duplicada, Opção duplicada",
            "instante-invalido, deve ser um instante ISO-8601 válido",
            "periodo-invertido, Intervalo inválido",
            "papel-invalido, --role deve ser SOLICITANTE",
            "professor-vazio, --professor-id não pode ser vazio"
    })
    void invalidArgumentsFailBeforeMakingRequests(String scenario, String expected) {
        String[] arguments = switch (scenario) {
            case "uso" -> new String[] {};
            case "invalido" -> new String[] {"unknown", "command"};
            case "sem-user" -> new String[] {"resources", "list"};
            case "par-errado" -> new String[] {"resources", "list", "--user"};
            case "opcao-nao-suportada" -> new String[] {"resources", "list", "--user", "teacher", "--x", "y"};
            case "opcao-duplicada" -> new String[] {"resources", "list", "--user", "teacher", "--user", "again"};
            case "instante-invalido" -> new String[] {"resources", "availability", "--user", "teacher",
                    "--resource-id", "room", "--starts-at", "bad", "--ends-at", "2026-11-10T11:00:00Z"};
            case "periodo-invertido" -> new String[] {"reservations", "create", "--user", "teacher",
                    "--resource-id", "room", "--starts-at", "2026-11-10T11:00:00Z",
                    "--ends-at", "2026-11-10T10:00:00Z"};
            case "papel-invalido" -> new String[] {"users", "create", "--user", "admin", "--login", "u",
                    "--role", "STUDENT"};
            case "professor-vazio" -> new String[] {"resources", "availability", "--user", "teacher",
                    "--resource-id", "room", "--starts-at", "2026-11-10T10:00:00Z",
                    "--ends-at", "2026-11-10T11:00:00Z", "--professor-id", " "};
            default -> throw new IllegalStateException("Unknown test scenario");
        };

        Result result = run(arguments, "");

        assertFailure(result, expected);
        assertTrue(requests.isEmpty());
    }

    @Test
    void missingStdinPasswordAndUnreachableApiFailSafely() {
        assertFailure(run("resources", "list", "--user", "teacher", ""),
                "A senha deve ser fornecida por stdin");

        int unavailable = ProductCli.run(
                new String[] {"resources", "list", "--user", "teacher"},
                input(PASSWORD + "\n"), new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(new ByteArrayOutputStream()), Map.of("RESOURCE_API_URL", "http://127.0.0.1:1"));
        assertEquals(2, unavailable);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String path = exchange.getRequestURI().getRawPath();
        String fullPath = path + (exchange.getRequestURI().getRawQuery() == null
                ? "" : "?" + exchange.getRequestURI().getRawQuery());
        requests.add(new Request(exchange.getRequestMethod(), fullPath, body,
                exchange.getRequestHeaders().getFirst("Authorization")));
        Response response = responses.getOrDefault(exchange.getRequestMethod() + " " + fullPath,
                exchange.getRequestMethod().equals("POST") && fullPath.equals("/api/auth/login")
                        ? new Response(200, "{\"accessToken\":\"" + TOKEN + "\"}")
                        : new Response(200, "{}"));
        byte[] content = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(response.status(), content.length);
        try (var output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    private void respond(String method, String path, int status, String body) {
        responses.put(method + " " + path, new Response(status, body));
    }

    private Request request(String method, String path) {
        return requests.stream()
                .filter(request -> request.method().equals(method) && request.path().equals(path))
                .findFirst()
                .orElseThrow();
    }

    private Result run(String... argumentsAndInput) {
        String input = argumentsAndInput[argumentsAndInput.length - 1];
        String[] arguments = java.util.Arrays.copyOf(argumentsAndInput, argumentsAndInput.length - 1);
        return run(arguments, input);
    }

    private Result run(String[] arguments, String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        int exitCode = ProductCli.run(arguments, input(input), new java.io.PrintStream(output),
                new java.io.PrintStream(error), Map.of("RESOURCE_API_URL", baseUrl));
        return new Result(exitCode, output.toString(StandardCharsets.UTF_8) + error.toString(StandardCharsets.UTF_8));
    }

    private static ByteArrayInputStream input(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertSuccess(Result result) {
        assertEquals(0, result.exitCode());
    }

    private static void assertFailure(Result result, String expectedMessage) {
        assertEquals(2, result.exitCode());
        assertTrue(result.output().contains(expectedMessage), result.output());
        assertFalse(result.output().contains("Exception"), result.output());
    }

    private record Request(String method, String path, String body, String authorization) {
    }

    private record Response(int status, String body) {
    }

    private record Result(int exitCode, String output) {
    }
}