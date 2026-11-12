package br.com.organizacao.recursos.reservas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Um caso de teste por linha (operação e cenário) de docs/seguranca/matriz-autorizacao.md. Cada caso prepara os
 * dados pela API (com os perfis certos) e devolve a requisição que o perfil sob teste vai executar.
 */
final class AuthorizationMatrixCases {
    private static final String OWN = "reserva própria";
    private static final String OTHERS = "reserva alheia";
    private static final String DEFAULT = "padrão";

    private AuthorizationMatrixCases() {
    }

    /** Dados e identidades de uma execução (um login por usuário, reaproveitado). */
    static final class Setup {
        private static final AtomicInteger SEQUENCE = new AtomicInteger();
        private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();

        private final MockMvc mockMvc;
        private final String credential;

        Setup(MockMvc mockMvc, String credential) {
            this.mockMvc = mockMvc;
            this.credential = credential;
        }

        String credential() {
            return credential;
        }

        String token(String username) throws Exception {
            String cached = TOKENS.get(username);
            if (cached != null) {
                return cached;
            }
            String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + username + "\",\"password\":\"" + credential + "\"}"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String token = JsonPath.read(body, "$.access_token");
            TOKENS.put(username, token);
            return token;
        }

        /** Cria, uma vez, um segundo Solicitante para os cenários de reserva alheia. */
        void ensureOtherRequester() throws Exception {
            if (TOKENS.containsKey("outro")) {
                return;
            }
            mockMvc.perform(json(post("/api/users"), "admin", "{\"username\":\"outro\",\"password\":\"" + credential
                            + "\",\"displayName\":\"Outro Solicitante\",\"role\":\"SOLICITANTE\"}"))
                    .andExpect(status().isCreated());
        }

        MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String username, String body)
                throws Exception {
            return builder.header("Authorization", "Bearer " + token(username))
                    .contentType(MediaType.APPLICATION_JSON).content(body);
        }

        String uniqueName(String prefix) {
            return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        }

        /** Dia exclusivo de cada fixture, para que reservas do mesmo usuário nunca se sobreponham. */
        String nextDay() {
            return LocalDate.of(2032, 1, 1).plusDays(SEQUENCE.incrementAndGet()).toString();
        }

        String resource(String type, boolean restricted) throws Exception {
            String id = uniqueName("matriz");
            mockMvc.perform(json(post("/api/resources"), "admin", "{\"id\":\"" + id + "\",\"capacity\":10,\"type\":\""
                            + type + "\",\"restricted\":" + restricted + "}")).andExpect(status().isCreated());
            return id;
        }

        long reservation(String owner, boolean restricted) throws Exception {
            ensureOtherRequester();
            String resource = resource("SALA", restricted);
            String day = nextDay();
            String body = mockMvc.perform(json(post("/api/reservations"), owner, "{\"resourceId\":\"" + resource
                            + "\",\"startsAt\":\"" + day + "T10:00:00Z\",\"endsAt\":\"" + day + "T11:00:00Z\"}"))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            return ((Number) JsonPath.read(body, "$.reservationId")).longValue();
        }

        long startedReservation(String owner) throws Exception {
            long id = reservation(owner, false);
            mockMvc.perform(post("/api/reservations/" + id + "/start").header("Authorization", "Bearer " + token(owner)))
                    .andExpect(status().isOk());
            return id;
        }

        long loan() throws Exception {
            String material = resource("MATERIAL", false);
            String body = mockMvc.perform(json(post("/api/materials/" + material + "/withdrawals"), "responsavel",
                            "{\"borrowerId\":\"matriz\"}")).andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            return ((Number) JsonPath.read(body, "$.id")).longValue();
        }

        String period(String day) {
            return "{\"startsAt\":\"" + day + "T08:00:00Z\",\"endsAt\":\"" + day + "T09:00:00Z\"}";
        }
    }

    @FunctionalInterface
    interface Case {
        MockHttpServletRequestBuilder build(Setup setup) throws Exception;
    }

    private static String key(String operation, String scenario) {
        return operation + "|" + scenario;
    }

    /** Chave = operação normalizada + "|" + cenário, igual à de {@link AuthorizationMatrixDocument.Row#key()}. */
    static Map<String, Case> cases() {
        Map<String, Case> cases = new LinkedHashMap<>();
        cases.put(key("POST /api/auth/login", DEFAULT), s -> post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"solicitante\",\"password\":\"" + s.credential() + "\"}"));
        cases.put(key("GET /api/auth/me", DEFAULT), s -> get("/api/auth/me"));
        cases.put(key("GET /api/users", DEFAULT), s -> get("/api/users"));
        cases.put(key("POST /api/users", DEFAULT), s -> post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + s.uniqueName("usuario") + "\",\"password\":\"" + s.credential()
                        + "\",\"displayName\":\"Usuário da matriz\",\"role\":\"SOLICITANTE\"}"));
        cases.put(key("GET /api/resources", DEFAULT), s -> get("/api/resources"));
        cases.put(key("GET /api/resources/{}", DEFAULT), s -> get("/api/resources/" + s.resource("SALA", false)));
        cases.put(key("POST /api/resources", DEFAULT), s -> post("/api/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"" + s.uniqueName("novo") + "\",\"capacity\":5}"));
        cases.put(key("PUT /api/resources/{}", DEFAULT), s -> {
            String id = s.resource("SALA", false);
            return put("/api/resources/" + id).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":\"" + id + "\",\"capacity\":6}");
        });
        cases.put(key("GET /api/resources/{}/availability", DEFAULT), s -> get("/api/resources/"
                + s.resource("SALA", false) + "/availability")
                .queryParam("startsAt", "2032-01-01T10:00:00Z").queryParam("endsAt", "2032-01-01T11:00:00Z"));
        cases.put(key("GET /api/maintenance", DEFAULT), s -> get("/api/maintenance"));
        cases.put(key("POST /api/resources/{}/maintenance", DEFAULT), s -> post("/api/resources/"
                + s.resource("SALA", false) + "/maintenance").contentType(MediaType.APPLICATION_JSON)
                .content(s.period(s.nextDay())));
        cases.put(key("DELETE /api/resources/{}/maintenance", DEFAULT), s -> {
            String id = s.resource("SALA", false);
            s.mockMvc.perform(s.json(post("/api/resources/" + id + "/maintenance"), "admin", s.period(s.nextDay())))
                    .andExpect(status().isCreated());
            return delete("/api/resources/" + id + "/maintenance");
        });
        cases.put(key("GET /api/reservations", DEFAULT), s -> get("/api/reservations"));
        cases.put(key("POST /api/reservations", DEFAULT), s -> {
            String day = s.nextDay();
            return post("/api/reservations").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"resourceId\":\"" + s.resource("SALA", false) + "\",\"startsAt\":\"" + day
                            + "T10:00:00Z\",\"endsAt\":\"" + day + "T11:00:00Z\"}");
        });
        cases.put(key("GET /api/reservations/restricted", DEFAULT), s -> get("/api/reservations/restricted"));
        cases.put(key("POST /api/reservations/restricted", DEFAULT), s -> post("/api/reservations/restricted"));
        cases.put(key("GET /api/reservations/{}", DEFAULT), s -> get("/api/reservations/" + s.reservation("outro", false)));
        cases.put(key("GET /api/reservations/{}/history", DEFAULT), s -> get("/api/reservations/"
                + s.reservation("outro", false) + "/history"));
        cases.put(key("PUT /api/reservations/{}", OWN), s -> update(s, "solicitante"));
        cases.put(key("PUT /api/reservations/{}", OTHERS), s -> update(s, "outro"));
        cases.put(key("DELETE /api/reservations/{}", OWN), s -> delete("/api/reservations/" + s.reservation("solicitante", false)));
        cases.put(key("DELETE /api/reservations/{}", OTHERS), s -> delete("/api/reservations/" + s.reservation("outro", false)));
        cases.put(key("POST /api/reservations/{}/approval", DEFAULT), s -> post("/api/reservations/"
                + s.reservation("outro", true) + "/approval"));
        cases.put(key("POST /api/reservations/{}/rejection", DEFAULT), s -> post("/api/reservations/"
                + s.reservation("outro", true) + "/rejection"));
        cases.put(key("POST /api/reservations/{}/no-show", DEFAULT), s -> post("/api/reservations/"
                + s.reservation("outro", false) + "/no-show"));
        cases.put(key("POST /api/reservations/{}/start", OWN), s -> post("/api/reservations/"
                + s.reservation("solicitante", false) + "/start"));
        cases.put(key("POST /api/reservations/{}/start", OTHERS), s -> post("/api/reservations/"
                + s.reservation("outro", false) + "/start"));
        cases.put(key("POST /api/reservations/{}/complete", OWN), s -> post("/api/reservations/"
                + s.startedReservation("solicitante") + "/complete"));
        cases.put(key("POST /api/reservations/{}/complete", OTHERS), s -> post("/api/reservations/"
                + s.startedReservation("outro") + "/complete"));
        cases.put(key("POST /api/materials/{}/withdrawals", DEFAULT), s -> post("/api/materials/"
                + s.resource("MATERIAL", false) + "/withdrawals").contentType(MediaType.APPLICATION_JSON)
                .content("{\"borrowerId\":\"matriz\"}"));
        cases.put(key("POST /api/loans/{}/return", DEFAULT), s -> post("/api/loans/" + s.loan() + "/return"));
        cases.put(key("GET /api/loans/{}", DEFAULT), s -> get("/api/loans/" + s.loan()));
        cases.put(key("GET /api/loans", DEFAULT), s -> get("/api/loans"));
        cases.put(key("GET /api/notifications", DEFAULT), s -> get("/api/notifications"));
        cases.put(key("GET /relatorios/utilizacao", DEFAULT), s -> get("/relatorios/utilizacao")
                .queryParam("inicio", "2032-01-01T00:00:00Z").queryParam("fim", "2033-01-01T00:00:00Z"));
        return cases;
    }

    private static MockHttpServletRequestBuilder update(Setup s, String owner) throws Exception {
        long id = s.reservation(owner, false);
        String day = s.nextDay();
        return put("/api/reservations/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"startsAt\":\"" + day + "T14:00:00Z\",\"endsAt\":\"" + day + "T15:00:00Z\"}");
    }
}
