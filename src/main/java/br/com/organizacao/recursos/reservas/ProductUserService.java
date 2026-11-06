package br.com.organizacao.recursos.reservas;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("!perf & !test")
public class ProductUserService {
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(15);

    private final ProductUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    public ProductUserService(
            ProductUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
    }

    @Transactional
    public void bootstrapAdministrator(String login, String password) {
        userRepository.lockAdministratorBootstrap();
        if (userRepository.hasAdministrator()) {
            return;
        }
        if (login == null || login.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_LOGIN and BOOTSTRAP_ADMIN_PASSWORD are required before the first administrator exists");
        }
        userRepository.insert(login, passwordEncoder.encode(password), ProductUserRole.ADMINISTRADOR);
    }

    public TokenResponse authenticate(String login, String password) {
        ProductUser user = userRepository.findByLogin(login)
                .filter(ProductUser::active)
                .filter(candidate -> passwordEncoder.matches(password, candidate.passwordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid login or password"));

        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("organizacao-de-recursos")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(TOKEN_LIFETIME))
                .subject(user.login())
                .claim("roles", user.roles().stream().map(ProductUserRole::name).toList())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", TOKEN_LIFETIME.toSeconds());
    }

    @Transactional
    public void createUser(String login, String password, ProductUserRole role) {
        try {
            userRepository.insert(login, passwordEncoder.encode(password), role);
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Login already exists");
        }
    }

    public List<ProductUserRole> currentRoles(String login) {
        return userRepository.findByLogin(login)
                .filter(ProductUser::active)
                .map(ProductUser::roles)
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(
                        "Invalid bearer token"));
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }
}