package br.com.organizacao.recursos.reservas;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

/**
 * Cria usuarios e recursos de demonstracao quando {@code app.demo-data.enabled=true} e o banco ainda
 * nao tem usuarios. Nao use em producao: as senhas de demonstracao sao conhecidas.
 */
@Component
@Profile("!perf")
public class DemoDataInitializer implements ApplicationRunner {
    static final String DEMO_PASSWORD = "Senha@123";

    private final boolean enabled;
    private final UserStore userStore;
    private final ResourceStore resourceStore;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(
            @Value("${app.demo-data.enabled:false}") boolean enabled,
            UserStore userStore,
            ResourceStore resourceStore,
            PasswordEncoder passwordEncoder) {
        this.enabled = enabled;
        this.userStore = userStore;
        this.resourceStore = resourceStore;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || userStore.count() > 0) {
            return;
        }
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        List.of(
                new AppUser("admin", hash, "Ana Administradora", UserRole.ADMINISTRADOR, true),
                new AppUser("responsavel", hash, "Rui Responsavel", UserRole.RESPONSAVEL, true),
                new AppUser("solicitante", hash, "Sara Solicitante", UserRole.SOLICITANTE, true))
                .forEach(userStore::create);
        List.of(
                new Resource("sala-101", "Sala 101", ResourceType.SALA, 40, "Bloco A", null, false),
                new Resource("lab-informatica", "Laboratorio de Informatica", ResourceType.SALA, 25,
                        "Bloco B", null, true),
                new Resource("prof-silva", "Prof. Silva", ResourceType.PROFESSOR, 1, "Bloco A",
                        "Matematica", false),
                new Resource("projetor-01", "Projetor 01", ResourceType.MATERIAL, 1, "Almoxarifado",
                        null, false))
                .forEach(resource -> {
                    if (resourceStore.findById(resource.id()).isEmpty()) {
                        resourceStore.create(resource);
                    }
                });
    }
}
