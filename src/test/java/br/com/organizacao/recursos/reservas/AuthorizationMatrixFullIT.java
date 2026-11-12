package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Percorre <b>cada célula</b> de docs/seguranca/matriz-autorizacao.md (linha × perfil) contra a API real, com
 * PostgreSQL e o JWT emitido pelo login de cada perfil. As linhas vêm do próprio documento.
 */
@Testcontainers
@SpringBootTest(properties = "app.demo-data.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
class AuthorizationMatrixFullIT {
    private static final String CREDENTIAL = UUID.randomUUID().toString();
    private static final Pattern INTERNAL_DETAILS = Pattern.compile(
            "(?is)\\bat [\\w.$]+\\([^)]*\\.java:\\d+\\)|org\\.springframework|br\\.com\\.organizacao|\\bSELECT\\b|"
                    + "\\bINSERT\\b|Exception");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("resource_reservations")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("app.demo-data.password", () -> CREDENTIAL);
    }

    static Stream<Arguments> cells() throws Exception {
        return AuthorizationMatrixDocument.rows().stream().flatMap(row ->
                Stream.of(AuthorizationMatrixDocument.Persona.values()).map(persona -> Arguments.of(
                        Named.of(row + " — " + persona + " → " + row.expected(persona), row), persona)));
    }

    @ParameterizedTest
    @MethodSource("cells")
    void apiMatchesTheAuthorizationMatrix(
            AuthorizationMatrixDocument.Row row, AuthorizationMatrixDocument.Persona persona) throws Exception {
        AuthorizationMatrixCases.Setup setup = new AuthorizationMatrixCases.Setup(mockMvc, CREDENTIAL);
        Map<String, AuthorizationMatrixCases.Case> cases = AuthorizationMatrixCases.cases();
        MockHttpServletRequestBuilder request = cases.get(row.key()).build(setup);
        if (persona.username() != null) {
            request.header("Authorization", "Bearer " + setup.token(persona.username()));
        }

        MvcResult result = mockMvc.perform(request).andReturn();

        assertEquals(row.expected(persona), result.getResponse().getStatus(),
                row + " como " + persona + ": " + result.getResponse().getContentAsString());
        if (result.getResponse().getStatus() >= 400) {
            assertFalse(INTERNAL_DETAILS.matcher(result.getResponse().getContentAsString()).find(),
                    "A resposta de erro expõe detalhes internos: " + result.getResponse().getContentAsString());
        }
    }
}
