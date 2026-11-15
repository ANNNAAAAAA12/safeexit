package safeexit_backend.controladores;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import safeexit_backend.dto.LoginRequest;
import safeexit_backend.dto.TokenResponse;
import safeexit_backend.servicios.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse iniciarSesion(@Valid @RequestBody LoginRequest request) {
        return authService.iniciarSesion(request);
    }
}
