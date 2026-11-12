package br.com.organizacao.recursos.reservas;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Protege o RNF-05: todo endpoint mapeado no código precisa aparecer em docs/api.md.
 * Os nomes das variáveis de caminho são ignorados ({id} e {resourceId} equivalem).
 */
@SpringBootTest
class ApiDocumentationTest {
    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void everyMappedEndpointIsDocumented() throws Exception {
        String documentation = normalize(Files.readString(Path.of("docs/api.md")));
        Set<String> missing = new TreeSet<>();
        for (RequestMappingInfo mapping : handlerMapping.getHandlerMethods().keySet()) {
            for (String pattern : mapping.getPathPatternsCondition().getPatternValues()) {
                boolean owned = pattern.startsWith("/api/") || pattern.startsWith("/relatorios/");
                if (owned && !documentation.contains(normalize(pattern))) {
                    missing.add(pattern);
                }
            }
        }
        assertTrue(missing.isEmpty(), "Endpoints sem documentação em docs/api.md: " + missing);
    }

    @Test
    void documentationOfTheReportEndpointExistsEvenWithoutThePostgresProfile() throws Exception {
        String documentation = Files.readString(Path.of("docs/api.md"));
        assertTrue(documentation.contains("/relatorios/utilizacao"));
    }

    private static String normalize(String text) {
        return text.replaceAll("\\{[^}/]*}", "{}");
    }
}
