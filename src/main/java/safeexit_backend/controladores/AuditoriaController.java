package safeexit_backend.controladores;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import safeexit_backend.dto.CacheEstadoResponse;
import safeexit_backend.dto.LecturaAuditoriaResponse;
import safeexit_backend.dto.NotificacionAuditoriaResponse;
import safeexit_backend.servicios.AuditoriaService;
import safeexit_backend.servicios.VehiculoValidacionCache;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;
    private final VehiculoValidacionCache cache;

    public AuditoriaController(AuditoriaService auditoriaService, VehiculoValidacionCache cache) {
        this.auditoriaService = auditoriaService;
        this.cache = cache;
    }

    @GetMapping("/lecturas")
    public List<LecturaAuditoriaResponse> listarLecturas() {
        return auditoriaService.listarLecturas();
    }

    @GetMapping("/lecturas/{id}/imagen")
    public ResponseEntity<byte[]> obtenerImagen(@PathVariable Long id) {
        var imagen = auditoriaService.obtenerImagen(id);
        MediaType tipoContenido = MediaType.parseMediaType(imagen.tipoContenido());
        return ResponseEntity.ok().contentType(tipoContenido).body(imagen.bytes());
    }

    @GetMapping("/notificaciones")
    public List<NotificacionAuditoriaResponse> listarNotificaciones() {
        return auditoriaService.listarNotificaciones();
    }

    @GetMapping("/cache")
    public CacheEstadoResponse estadoCache() {
        return cache.estado();
    }
}
