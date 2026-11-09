package br.com.organizacao.recursos.reservas;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Login por usuario/senha e emissao do JWT usado pelo frontend (RF-01). */
@RestController
@Profile("!perf")
public class AuthController {
    private final UserStore userStore;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final long tokenLifetimeSeconds;
    private final String dummyHash;

    public AuthController(
            UserStore userStore,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            Clock clock,
            @Value("${app.security.token-lifetime-seconds:28800}") long tokenLifetimeSeconds) {
        this.userStore = userStore;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.tokenLifetimeSeconds = tokenLifetimeSeconds;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @PostMapping("/api/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AppUser user = userStore.findByUsername(request.username()).orElse(null);
        // Compara sempre um hash para nao revelar, pelo tempo de resposta, se o usuario existe.
        boolean passwordMatches = passwordEncoder.matches(
                request.password(), user == null ? dummyHash : user.passwordHash());
        if (user == null || !user.active() || !passwordMatches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.username())
                .claim("roles", List.of(user.role().name()))
                .claim("name", user.displayName())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(tokenLifetimeSeconds))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", tokenLifetimeSeconds,
                new UserInfo(user.username(), user.displayName(), user.role()));
    }

    @GetMapping("/api/auth/me")
    @PreAuthorize("isAuthenticated()")
    public UserInfo me(Authentication authentication) {
        AppUser user = userStore.findByUsername(authentication.getName())
                .filter(AppUser::active)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        return new UserInfo(user.username(), user.displayName(), user.role());
    }

    public record LoginRequest(
            @NotBlank(message = "username is required") String username,
            @NotBlank(message = "password is required") String password) {
    }

    public record UserInfo(String username, String displayName, UserRole role) {
    }

    public record LoginResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn,
            UserInfo user) {
    }
}
