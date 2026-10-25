package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationClockConfiguration {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
