package br.com.organizacao.recursos.reservas;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguranca da aplicacao (RF-01): API stateless protegida por JWT HS256 com o claim "roles",
 * o mesmo formato usado pelo perfil de performance. Os arquivos estaticos do frontend sao publicos;
 * toda a API exige autenticacao, exceto o login.
 */
@Configuration
@Profile("!perf")
public class AppSecurityConfiguration {
    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger(AppSecurityConfiguration.class);

    @Bean
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/index.html", "/favicon.ico", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return authenticationConverter;
    }

    /**
     * Chave HS256 do JWT. Vem de {@code app.security.jwt-secret} (variavel JWT_SECRET); sem ela, gera-se uma chave
     * aleatoria a cada inicializacao, o que invalida os tokens ao reiniciar e nao serve para varias instancias.
     */
    @Bean
    SecretKey applicationJwtKey(@Value("${app.security.jwt-secret:}") String secret) {
        if (secret.isBlank()) {
            LOGGER.warn("app.security.jwt-secret nao definido: usando chave aleatoria desta execucao. "
                    + "Defina JWT_SECRET em ambientes reais.");
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            return new SecretKeySpec(random, "HmacSHA256");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.security.jwt-secret must have at least 32 bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder applicationJwtEncoder(SecretKey applicationJwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(applicationJwtKey));
    }

    @Bean
    JwtDecoder applicationJwtDecoder(SecretKey applicationJwtKey) {
        return NimbusJwtDecoder.withSecretKey(applicationJwtKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
