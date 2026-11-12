package br.com.organizacao.recursos.reservas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lê as linhas da tabela de docs/seguranca/matriz-autorizacao.md: a matriz é a fonte única, sem cópia no código. */
final class AuthorizationMatrixDocument {
    static final Path FILE = Path.of("docs/seguranca/matriz-autorizacao.md");
    private static final Pattern ROW = Pattern.compile(
            "^\\| `([A-Z]+ [^`]+)` \\| ([^|]+?) \\| (\\d{3}) \\| (\\d{3}) \\| (\\d{3}) \\| (\\d{3}) \\|.*$");

    enum Persona {
        ANONIMO(null), SOLICITANTE("solicitante"), RESPONSAVEL("responsavel"), ADMINISTRADOR("admin");

        private final String username;

        Persona(String username) {
            this.username = username;
        }

        String username() {
            return username;
        }
    }

    record Row(String operation, String scenario, int anonymous, int solicitante, int responsible, int administrator) {
        String key() {
            return normalize(operation) + "|" + scenario;
        }

        int expected(Persona persona) {
            return switch (persona) {
                case ANONIMO -> anonymous;
                case SOLICITANTE -> solicitante;
                case RESPONSAVEL -> responsible;
                case ADMINISTRADOR -> administrator;
            };
        }

        @Override
        public String toString() {
            return operation + " [" + scenario + "]";
        }
    }

    private AuthorizationMatrixDocument() {
    }

    static List<Row> rows() throws IOException {
        List<Row> rows = new ArrayList<>();
        for (String line : Files.readAllLines(FILE)) {
            Matcher matcher = ROW.matcher(line);
            if (matcher.matches()) {
                rows.add(new Row(matcher.group(1), matcher.group(2), Integer.parseInt(matcher.group(3)),
                        Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)),
                        Integer.parseInt(matcher.group(6))));
            }
        }
        return rows;
    }

    /** Os nomes das variáveis de caminho não importam: {id} e {resourceId} equivalem. */
    static String normalize(String operation) {
        return operation.replaceAll("\\{[^}/]*}", "{}");
    }
}
