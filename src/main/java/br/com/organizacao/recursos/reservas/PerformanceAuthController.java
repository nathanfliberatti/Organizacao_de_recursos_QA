package br.com.organizacao.recursos.reservas;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

@RestController
@Profile("perf")
public class PerformanceAuthController {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("perf-user-(\\d{2})");
    private static final int TOKEN_LIFETIME_SECONDS = 1200;

    private final JwtEncoder jwtEncoder;

    public PerformanceAuthController(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Matcher username = request.username() == null ? null : USERNAME_PATTERN.matcher(request.username());
        if (username == null || !username.matches() || !validCredentials(request, username.group(1))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(request.username())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(TOKEN_LIFETIME_SECONDS))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return ResponseEntity.ok(new LoginResponse(token, "Bearer", TOKEN_LIFETIME_SECONDS));
    }

    private boolean validCredentials(LoginRequest request, String userNumber) {
        if (Integer.parseInt(userNumber) < 1 || Integer.parseInt(userNumber) > 50 || request.password() == null) {
            return false;
        }
        String expectedPassword = "local-only-placeholder-" + userNumber;
        return MessageDigest.isEqual(
                expectedPassword.getBytes(StandardCharsets.UTF_8),
                request.password().getBytes(StandardCharsets.UTF_8));
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn) {
    }
}