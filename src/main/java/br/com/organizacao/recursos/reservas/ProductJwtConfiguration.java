package br.com.organizacao.recursos.reservas;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@Profile("!perf & !test")
public class ProductJwtConfiguration {
    @Bean
    SecretKey productJwtSecretKey(Environment environment) {
        String configuredSecret = environment.getProperty("RECURSOS_JWT_SECRET");
        if (configuredSecret == null || configuredSecret.isEmpty()) {
            throw new IllegalStateException("RECURSOS_JWT_SECRET must contain at least 32 bytes");
        }
        byte[] secretBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("RECURSOS_JWT_SECRET must contain at least 32 bytes");
        }
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder productJwtEncoder(SecretKey productJwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(productJwtSecretKey));
    }

    @Bean
    JwtDecoder productJwtDecoder(SecretKey productJwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(productJwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}