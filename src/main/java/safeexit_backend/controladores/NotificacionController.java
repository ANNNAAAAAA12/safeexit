package safeexit_backend.controladores;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import safeexit_backend.dto.NotificacionPruebaRequest;
import safeexit_backend.modelos.EstadoEntrega;
import safeexit_backend.servicios.NotificacionService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notificaciones/whatsapp")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @PostMapping("/prueba")
    public ResponseEntity<Map<String, String>> enviarPrueba(
            @AuthenticationPrincipal UserDetails usuario,
            @Valid @RequestBody NotificacionPruebaRequest request) {
        EstadoEntrega estado = notificacionService.enviarPrueba(usuario.getUsername(), request.mensaje());
        return ResponseEntity.ok(Map.of("estado", estado.name()));
    }
}
