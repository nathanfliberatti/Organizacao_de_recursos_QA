package br.com.organizacao.recursos.reservas;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Profile("!perf & !test")
public class ProductUserController {
    private final ProductUserService userService;

    public ProductUserController(ProductUserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public UserCreatedResponse create(@Valid @RequestBody CreateProductUserRequest request) {
        ProductUserRole role;
        try {
            role = ProductUserRole.valueOf(request.role());
        } catch (IllegalArgumentException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "role must be SOLICITANTE, RESPONSAVEL or ADMINISTRADOR");
        }
        userService.createUser(request.login(), request.password(), role);
        return new UserCreatedResponse(request.login(), role.name(), true);
    }

    record CreateProductUserRequest(
            @NotBlank(message = "login is required") String login,
            @NotBlank(message = "password is required") String password,
            @NotBlank(message = "role is required") String role) {
    }

    public record UserCreatedResponse(String login, String role, boolean active) {
    }
}