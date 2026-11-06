package br.com.organizacao.recursos.reservas;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("!perf & !test")
public class ProductAdminBootstrap implements ApplicationRunner {
    private final ProductUserService userService;
    private final Environment environment;

    public ProductAdminBootstrap(ProductUserService userService, Environment environment) {
        this.userService = userService;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        userService.bootstrapAdministrator(
                environment.getProperty("BOOTSTRAP_ADMIN_LOGIN"),
                environment.getProperty("BOOTSTRAP_ADMIN_PASSWORD"));
    }
}