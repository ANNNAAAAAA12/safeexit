package safeexit_backend.controladores;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import safeexit_backend.dto.AlertaResponse;
import safeexit_backend.servicios.AlertaService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alertas")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    @GetMapping("/historial")
    public List<AlertaResponse> historial(@AuthenticationPrincipal UserDetails usuario) {
        boolean administrador = usuario.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return alertaService.historial(usuario.getUsername(), administrador);
    }
}
