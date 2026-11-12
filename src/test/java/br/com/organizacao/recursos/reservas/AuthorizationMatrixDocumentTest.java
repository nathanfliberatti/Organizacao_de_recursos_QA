package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Prende o documento da matriz de autorização ao código: toda rota mapeada precisa ter linha na tabela,
 * toda linha precisa ser uma rota real e ter caso de teste. Quem adiciona uma rota sem atualizar a matriz
 * reprova aqui.
 */
@SpringBootTest
class AuthorizationMatrixDocumentTest {
    /** Existe somente no perfil postgres, que este contexto (em memória) não carrega. */
    private static final Set<String> POSTGRES_ONLY_ROUTES = Set.of("GET /relatorios/utilizacao");
    private static final Set<Integer> CODES = Set.of(200, 201, 204, 401, 403);

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    private Set<String> mappedRoutes() {
        Set<String> routes = new TreeSet<>();
        for (RequestMappingInfo mapping : handlerMapping.getHandlerMethods().keySet()) {
            for (String pattern : mapping.getPathPatternsCondition().getPatternValues()) {
                if (!pattern.startsWith("/api/") && !pattern.startsWith("/relatorios/")) {
                    continue;
                }
                for (RequestMethod method : mapping.getMethodsCondition().getMethods()) {
                    routes.add(AuthorizationMatrixDocument.normalize(method.name() + " " + pattern));
                }
            }
        }
        return routes;
    }

    private Set<String> documentedRoutes() throws Exception {
        Set<String> routes = new TreeSet<>();
        AuthorizationMatrixDocument.rows().forEach(row ->
                routes.add(AuthorizationMatrixDocument.normalize(row.operation())));
        return routes;
    }

    @Test
    void everyMappedRouteIsInTheAuthorizationMatrix() throws Exception {
        Set<String> missing = new TreeSet<>(mappedRoutes());
        missing.removeAll(documentedRoutes());

        assertTrue(missing.isEmpty(), "Rotas sem linha em docs/seguranca/matriz-autorizacao.md: " + missing);
    }

    @Test
    void everyRowOfTheMatrixIsARealRoute() throws Exception {
        Set<String> unknown = new TreeSet<>(documentedRoutes());
        unknown.removeAll(mappedRoutes());
        unknown.removeAll(POSTGRES_ONLY_ROUTES);

        assertTrue(unknown.isEmpty(), "Linhas da matriz que não correspondem a nenhuma rota: " + unknown);
    }

    @Test
    void everyRowHasATestCaseAndEveryTestCaseHasARow() throws Exception {
        Set<String> documented = new TreeSet<>();
        AuthorizationMatrixDocument.rows().forEach(row -> documented.add(row.key()));

        assertEquals(documented, new TreeSet<>(AuthorizationMatrixCases.cases().keySet()));
    }

    @Test
    void rowsAreWellFormedAndNothingIsOpenToAnonymousExceptLogin() throws Exception {
        Set<String> keys = new TreeSet<>();
        for (AuthorizationMatrixDocument.Row row : AuthorizationMatrixDocument.rows()) {
            assertTrue(keys.add(row.key()), "Linha duplicada na matriz: " + row);
            for (AuthorizationMatrixDocument.Persona persona : AuthorizationMatrixDocument.Persona.values()) {
                int code = row.expected(persona);
                assertTrue(CODES.contains(code) || (code == 200 && row.operation().equals("POST /api/auth/login")),
                        "Código fora do conjunto permitido em " + row + ": " + code);
            }
            if (!row.operation().equals("POST /api/auth/login")) {
                assertEquals(401, row.anonymous(), "O anônimo deve receber 401 em " + row);
            }
        }
        assertTrue(keys.size() >= 35, "A matriz tem poucas linhas: " + keys.size());
    }
}
