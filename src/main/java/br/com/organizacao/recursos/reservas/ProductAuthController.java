package br.com.organizacao.recursos.reservas;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Profile("!perf & !test")
public class ProductAuthController {
    private final ProductUserService userService;

    public ProductAuthController(ProductUserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ProductUserService.TokenResponse login(@Valid @RequestBody ProductLoginRequest request) {
        return userService.authenticate(request.login(), request.password());
    }

    record ProductLoginRequest(
            @NotBlank(message = "login is required") String login,
            @NotBlank(message = "password is required") String password) {
    }
}