package br.com.organizacao.recursos.reservas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class ProductJwtConfigurationTest {
    private final ApplicationContextRunner productContext = new ApplicationContextRunner()
            .withUserConfiguration(ProductJwtConfiguration.class)
            .withInitializer(activeProfile("postgres"));

    @Test
    void missingSecretPreventsProductAuthenticationFromStarting() {
        productContext.run(context -> assertSecretFailure(context));
    }

    @Test
    void emptySecretPreventsProductAuthenticationFromStarting() {
        productContext.withPropertyValues("RECURSOS_JWT_SECRET=")
                .run(context -> assertSecretFailure(context));
    }

    @Test
    void secretShorterThanThirtyTwoBytesPreventsProductAuthenticationFromStarting() {
        productContext.withPropertyValues("RECURSOS_JWT_SECRET=short-secret")
                .run(context -> assertSecretFailure(context));
    }

    @Test
    void secretWithAtLeastThirtyTwoBytesCreatesJwtEncoderAndDecoder() {
        productContext.withPropertyValues("RECURSOS_JWT_SECRET=0123456789abcdef0123456789abcdef")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(JwtEncoder.class);
                    assertThat(context).hasSingleBean(JwtDecoder.class);
                });
    }

    @Test
    void perfProfileDoesNotLoadProductSecretConfiguration() {
        new ApplicationContextRunner()
                .withUserConfiguration(ProductJwtConfiguration.class)
                .withInitializer(activeProfile("perf"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(JwtEncoder.class);
                    assertThat(context).doesNotHaveBean(JwtDecoder.class);
                });
    }

    private static ApplicationContextInitializer<ConfigurableApplicationContext> activeProfile(String profile) {
        return context -> context.getEnvironment().setActiveProfiles(profile);
    }

    private static void assertSecretFailure(AssertableApplicationContext context) {
        assertThat(context.getStartupFailure())
            .isNotNull()
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("RECURSOS_JWT_SECRET must contain at least 32 bytes");
    }
}