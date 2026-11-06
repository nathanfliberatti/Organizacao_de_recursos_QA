package br.com.organizacao.recursos.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ProductCli {
    private static final String DEFAULT_API_URL = "http://localhost:8080";
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    private ProductCli() {
    }

    public static int run(String[] arguments) {
        return run(arguments, System.in, System.out, System.err, System.getenv());
    }

    static int run(
            String[] arguments,
            java.io.InputStream input,
            PrintStream output,
            PrintStream error,
            Map<String, String> environment) {
        try {
            Command command = Command.parse(arguments);
            String baseUrl = environment.getOrDefault("RESOURCE_API_URL", DEFAULT_API_URL)
                    .replaceAll("/+$", "");
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            String currentPassword = readPassword(reader, error, "Senha: ");
            HttpClient httpClient = HttpClient.newHttpClient();
            String token = authenticate(httpClient, baseUrl, command.user(), currentPassword);
            execute(command, httpClient, baseUrl, token, reader, output, error);
            return 0;
        } catch (CliException exception) {
            error.println(exception.getMessage());
            return 2;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            error.println("Execução interrompida.");
            return 2;
        } catch (IOException | RuntimeException exception) {
            error.println("Falha ao conectar à API. Verifique RESOURCE_API_URL e tente novamente.");
            return 2;
        }
    }

    private static void execute(
            Command command,
            HttpClient client,
            String baseUrl,
            String token,
            BufferedReader reader,
            PrintStream output,
            PrintStream error) throws IOException, InterruptedException {
        switch (command.operation()) {
            case "resources list" -> listResources(client, baseUrl, token, output, error);
            case "resources availability" -> availability(command, client, baseUrl, token, output, error);
            case "reservations create" -> createReservation(command, client, baseUrl, token, output, error);
            case "reservations list" -> listReservations(client, baseUrl, token, output, error);
            case "users create" -> createUser(command, client, baseUrl, token, reader, output, error);
            default -> throw new CliException("Comando inválido.");
        }
    }

    private static String authenticate(HttpClient client, String baseUrl, String login, String password)
            throws IOException, InterruptedException {
        ObjectNode body = JSON.createObjectNode().put("login", login).put("password", password);
        HttpResponse<String> response = send(client, baseUrl, "/api/auth/login", null, "POST", body);
        if (response.statusCode() == 401) {
            throw new CliException("Autenticação recusada. Verifique usuário e senha.");
        }
        if (response.statusCode() != 200) {
            throw apiFailure(response.statusCode());
        }
        JsonNode token = parse(response.body()).get("accessToken");
        if (token == null || token.asText().isBlank()) {
            throw new CliException("A API retornou uma resposta de autenticação inválida.");
        }
        return token.asText();
    }

    private static void listResources(
            HttpClient client, String baseUrl, String token, PrintStream output, PrintStream error)
            throws IOException, InterruptedException {
        HttpResponse<String> response = send(client, baseUrl, "/api/resources", token, "GET", null);
        requireSuccess(response, error);
        JsonNode resources = parse(response.body());
        output.println("ID\tCAPACIDADE");
        resources.forEach(resource -> output.printf("%s\t%d%n",
                resource.path("id").asText(), resource.path("capacity").asInt()));
    }

    private static void availability(
            Command command, HttpClient client, String baseUrl, String token, PrintStream output, PrintStream error)
            throws IOException, InterruptedException {
        String path = "/api/resources/" + path(command.required("resource-id")) + "/availability"
                + "?startsAt=" + query(command.required("starts-at"))
                + "&endsAt=" + query(command.required("ends-at"));
        if (command.options().containsKey("professor-id")) {
            path += "&professorId=" + query(command.options().get("professor-id"));
        }
        HttpResponse<String> response = send(client, baseUrl, path, token, "GET", null);
        if (response.statusCode() == 400) {
            throw new CliException("Intervalo inválido: --ends-at deve ser posterior a --starts-at.");
        }
        requireSuccess(response, error);
        output.println(parse(response.body()).path("available").asBoolean()
                ? "Disponível" : "Indisponível");
    }

    private static void createReservation(
            Command command, HttpClient client, String baseUrl, String token, PrintStream output, PrintStream error)
            throws IOException, InterruptedException {
        ObjectNode body = JSON.createObjectNode()
                .put("resourceId", command.required("resource-id"))
                .put("startsAt", command.required("starts-at"))
                .put("endsAt", command.required("ends-at"));
        HttpResponse<String> response = send(client, baseUrl, "/api/reservations", token, "POST", body);
        if (response.statusCode() == 400) {
            throw new CliException("Intervalo inválido: --ends-at deve ser posterior a --starts-at.");
        }
        if (response.statusCode() == 409) {
            throw new CliException("Conflito: o recurso ou professor já possui reserva nesse intervalo.");
        }
        requireSuccess(response, error);
        output.println("Reserva criada: " + parse(response.body()).path("reservationId").asText());
    }

    private static void listReservations(
            HttpClient client, String baseUrl, String token, PrintStream output, PrintStream error)
            throws IOException, InterruptedException {
        HttpResponse<String> response = send(client, baseUrl, "/api/reservations", token, "GET", null);
        requireSuccess(response, error);
        JsonNode reservations = parse(response.body());
        output.println("ID\tRECURSO\tINÍCIO\tFIM");
        reservations.forEach(reservation -> output.printf("%s\t%s\t%s\t%s%n",
                reservation.path("id").asText(),
                reservation.path("resourceId").asText(),
                reservation.path("startsAt").asText(),
                reservation.path("endsAt").asText()));
    }

    private static void createUser(
            Command command,
            HttpClient client,
            String baseUrl,
            String token,
            BufferedReader reader,
            PrintStream output,
            PrintStream error) throws IOException, InterruptedException {
        String newPassword = readPassword(reader, error, "Senha inicial do novo usuário: ");
        ObjectNode body = JSON.createObjectNode()
                .put("login", command.required("login"))
                .put("password", newPassword)
                .put("role", command.required("role"));
        HttpResponse<String> response = send(client, baseUrl, "/api/users", token, "POST", body);
        requireSuccess(response, error);
        JsonNode created = parse(response.body());
        output.printf("Usuário criado: %s (%s)%n",
                created.path("login").asText(), created.path("role").asText());
    }

    private static String readPassword(BufferedReader reader, PrintStream error, String prompt) throws IOException {
        error.print(prompt);
        error.flush();
        String password = reader.readLine();
        if (password == null || password.isEmpty()) {
            throw new CliException("A senha deve ser fornecida por stdin.");
        }
        return password;
    }

    private static HttpResponse<String> send(
            HttpClient client, String baseUrl, String path, String token, String method, ObjectNode body)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Accept", "application/json");
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        } else {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void requireSuccess(HttpResponse<String> response, PrintStream error) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw apiFailure(response.statusCode());
        }
    }

    private static CliException apiFailure(int status) {
        return switch (status) {
            case 401 -> new CliException("Autenticação necessária ou inválida.");
            case 403 -> new CliException("Permissão insuficiente para executar esta operação.");
            case 409 -> new CliException("Conflito: a operação não pode ser concluída com os dados atuais.");
            default -> status >= 400 && status < 500
                    ? new CliException("Requisição inválida. Verifique os argumentos informados.")
                    : new CliException("A API não conseguiu concluir a operação (HTTP " + status + ").");
        };
    }

    private static JsonNode parse(String body) {
        try {
            return JSON.readTree(body);
        } catch (IOException exception) {
            throw new CliException("A API retornou uma resposta inválida.");
        }
    }

    private static String path(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String query(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static Instant instant(String value, String option) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new CliException(option + " deve ser um instante ISO-8601 válido.");
        }
    }

    private static final class Command {
        private final String operation;
        private final Map<String, String> options;

        private Command(String operation, Map<String, String> options) {
            this.operation = operation;
            this.options = options;
        }

        private String operation() {
            return operation;
        }

        private String user() {
            return required("user");
        }

        private Map<String, String> options() {
            return options;
        }

        private String required(String option) {
            String value = options.get(option);
            if (value == null || value.isBlank()) {
                throw new CliException("Informe --" + option + ".");
            }
            return value;
        }

        private static Command parse(String[] arguments) {
            if (arguments.length < 2) {
                throw new CliException("Uso: cli <resources|reservations|users> <comando> [opções].");
            }
            String operation = arguments[0] + " " + arguments[1];
            Set<String> allowed = switch (operation) {
                case "resources list", "reservations list" -> Set.of("user");
                case "resources availability" -> Set.of(
                        "user", "resource-id", "starts-at", "ends-at", "professor-id");
                case "reservations create" -> Set.of("user", "resource-id", "starts-at", "ends-at");
                case "users create" -> Set.of("user", "login", "role");
                default -> throw new CliException("Comando inválido.");
            };
            Map<String, String> options = new LinkedHashMap<>();
            for (int index = 2; index < arguments.length; index += 2) {
                String argument = arguments[index];
                if (!argument.startsWith("--") || index + 1 >= arguments.length) {
                    throw new CliException("Cada opção deve usar o formato --opção valor.");
                }
                String option = argument.substring(2);
                if (!allowed.contains(option)) {
                    throw new CliException("Opção não suportada: --" + option + ".");
                }
                if (options.putIfAbsent(option, arguments[index + 1]) != null) {
                    throw new CliException("Opção duplicada: --" + option + ".");
                }
            }
            Command command = new Command(operation, options);
            command.user();
            switch (operation) {
                case "resources availability", "reservations create" -> {
                    command.required("resource-id");
                    Instant startsAt = instant(command.required("starts-at"), "--starts-at");
                    Instant endsAt = instant(command.required("ends-at"), "--ends-at");
                    if (!endsAt.isAfter(startsAt)) {
                        throw new CliException("Intervalo inválido: --ends-at deve ser posterior a --starts-at.");
                    }
                    if (options.containsKey("professor-id") && options.get("professor-id").isBlank()) {
                        throw new CliException("--professor-id não pode ser vazio.");
                    }
                }
                case "users create" -> {
                    command.required("login");
                    String role = command.required("role");
                    if (!Set.of("SOLICITANTE", "RESPONSAVEL", "ADMINISTRADOR").contains(role)) {
                        throw new CliException("--role deve ser SOLICITANTE, RESPONSAVEL ou ADMINISTRADOR.");
                    }
                }
                default -> {
                }
            }
            return command;
        }
    }

    private static final class CliException extends RuntimeException {
        private CliException(String message) {
            super(message);
        }
    }
}