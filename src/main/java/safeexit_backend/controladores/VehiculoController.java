package safeexit_backend.controladores;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import safeexit_backend.dto.CrearVehiculoRequest;
import safeexit_backend.dto.ActualizarDocumentosRequest;
import safeexit_backend.dto.VehiculoResponse;
import safeexit_backend.servicios.VehiculoService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;

    public VehiculoController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    @GetMapping("/mis-vehiculos")
    public List<VehiculoResponse> obtenerPropios(@AuthenticationPrincipal UserDetails usuario) {
        return vehiculoService.listarPropios(usuario.getUsername());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehiculoResponse crear(
            @AuthenticationPrincipal UserDetails usuario,
            @Valid @RequestBody CrearVehiculoRequest request) {
        return vehiculoService.crear(usuario.getUsername(), request);
    }

    @PatchMapping("/{id}/documentos")
    public VehiculoResponse actualizarDocumentos(
            @AuthenticationPrincipal UserDetails usuario,
            @PathVariable Long id,
            @Valid @RequestBody ActualizarDocumentosRequest request) {
        boolean administrador = usuario.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return vehiculoService.actualizarDocumentos(usuario.getUsername(), id, request, administrador);
    }
}