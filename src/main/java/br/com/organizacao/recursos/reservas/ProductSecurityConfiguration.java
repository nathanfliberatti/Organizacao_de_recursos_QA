package br.com.organizacao.recursos.reservas;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@Profile("!perf & !test")
public class ProductSecurityConfiguration {
    @Bean
    PasswordEncoder productPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain productSecurityFilterChain(
            HttpSecurity http,
            ProductUserService userService) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/login").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(token -> currentAuthentication(token, userService))))
                .build();
    }

    private JwtAuthenticationToken currentAuthentication(Jwt token, ProductUserService userService) {
        try {
            List<SimpleGrantedAuthority> authorities = userService.currentRoles(token.getSubject()).stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                    .toList();
            return new JwtAuthenticationToken(token, authorities, token.getSubject());
        } catch (BadCredentialsException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BadCredentialsException("Invalid bearer token", exception);
        }
    }
}