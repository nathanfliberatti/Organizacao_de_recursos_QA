package br.com.organizacao.recursos.reservas.cucumber;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@TestConfiguration(proxyBeanMethods = false)
@EnableMethodSecurity
public class CucumberMethodSecurityConfiguration {
}
