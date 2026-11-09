package br.com.organizacao.recursos.reservas;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Gestao de usuarios pelo Administrador. */
@RestController
@Profile("!perf")
public class UserController {
    private final UserStore userStore;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserStore userStore, PasswordEncoder passwordEncoder) {
        this.userStore = userStore;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/api/users")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<AuthController.UserInfo> list() {
        return userStore.findAll().stream()
                .map(user -> new AuthController.UserInfo(user.username(), user.displayName(), user.role()))
                .toList();
    }

    @PostMapping("/api/users")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AuthController.UserInfo> create(@Valid @RequestBody CreateUserRequest request) {
        AppUser user = userStore.create(new AppUser(
                request.username().trim(),
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                request.role(),
                true));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthController.UserInfo(user.username(), user.displayName(), user.role()));
    }

    public record CreateUserRequest(
            @NotBlank(message = "username is required") String username,
            @NotBlank(message = "password is required")
            @Size(min = 8, message = "password must have at least 8 characters") String password,
            @NotBlank(message = "displayName is required") String displayName,
            @NotNull(message = "role is required") UserRole role) {
    }
}
