package safeexit_backend.controladores;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import safeexit_backend.dto.ActualizarTelefonoRequest;
import safeexit_backend.dto.UsuarioResponse;
import safeexit_backend.servicios.UsuarioService;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PatchMapping("/me/telefono-whatsapp")
    public UsuarioResponse actualizarTelefono(
            @AuthenticationPrincipal UserDetails usuario,
            @Valid @RequestBody ActualizarTelefonoRequest request) {
        return usuarioService.actualizarTelefono(usuario.getUsername(), request.telefonoWhatsapp());
    }
}
