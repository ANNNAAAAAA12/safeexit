package safeexit_backend.controladores;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import safeexit_backend.dto.CambiarEstadoUsuarioRequest;
import safeexit_backend.dto.CrearUsuarioRequest;
import safeexit_backend.dto.ActualizarParametrosRequest;
import safeexit_backend.dto.ActualizarConjuntoRequest;
import safeexit_backend.dto.ConjuntoResponse;
import safeexit_backend.dto.ConsultaDocumentosResponse;
import safeexit_backend.dto.ProcesarSalidaResponse;
import safeexit_backend.dto.SimularLecturaRequest;
import safeexit_backend.dto.MetricasResponse;
import safeexit_backend.dto.UsuarioResponse;
import safeexit_backend.dto.ParametrosSistemaResponse;
import safeexit_backend.servicios.ParametrosSistemaService;
import safeexit_backend.servicios.MetricasService;
import safeexit_backend.servicios.ConjuntoResidencialService;
import safeexit_backend.servicios.UsuarioService;
import safeexit_backend.servicios.VehiculoService;
import safeexit_backend.servicios.ProcesamientoSalidaService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UsuarioService usuarioService;
    private final ParametrosSistemaService parametrosService;
    private final MetricasService metricasService;
    private final ConjuntoResidencialService conjuntoService;
    private final VehiculoService vehiculoService;
    private final ProcesamientoSalidaService procesamientoSalidaService;

    public AdminController(
            UsuarioService usuarioService,
            ParametrosSistemaService parametrosService,
            MetricasService metricasService,
            ConjuntoResidencialService conjuntoService,
            VehiculoService vehiculoService,
            ProcesamientoSalidaService procesamientoSalidaService) {
        this.usuarioService = usuarioService;
        this.parametrosService = parametrosService;
        this.metricasService = metricasService;
        this.conjuntoService = conjuntoService;
        this.vehiculoService = vehiculoService;
        this.procesamientoSalidaService = procesamientoSalidaService;
    }

    @GetMapping("/usuarios")
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody CrearUsuarioRequest request) {
        return usuarioService.crear(request);
    }

    @PatchMapping("/usuarios/{id}/estado")
    public UsuarioResponse cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoUsuarioRequest request) {
        return usuarioService.cambiarEstado(id, request.activo());
    }

    @PutMapping("/conjuntos/{conjuntoId}/parametros")
    public ParametrosSistemaResponse actualizarParametros(
            @PathVariable Long conjuntoId,
            @Valid @RequestBody ActualizarParametrosRequest request) {
        return parametrosService.actualizar(conjuntoId, request);
    }

    @GetMapping("/metricas")
    public MetricasResponse metricas() {
        return metricasService.obtener();
    }

    @GetMapping("/conjuntos/{conjuntoId}/parametros")
    public ParametrosSistemaResponse obtenerParametros(@PathVariable Long conjuntoId) {
        return parametrosService.obtener(conjuntoId);
    }

    @GetMapping("/conjuntos")
    public List<ConjuntoResponse> listarConjuntos() {
        return conjuntoService.listar();
    }

    @GetMapping("/demo/consulta-documentos/{placa}")
    public ConsultaDocumentosResponse consultarDocumentosDemo(
            @AuthenticationPrincipal UserDetails usuario,
            @PathVariable String placa) {
        return vehiculoService.consultarDocumentosDemo(usuario.getUsername(), placa);
    }

    @PostMapping("/demo/lecturas")
    public ProcesarSalidaResponse simularLectura(@Valid @RequestBody SimularLecturaRequest request) {
        return procesamientoSalidaService.procesarPlacaSimulada(request.placa());
    }

    @PutMapping("/conjuntos/{id}")
    public ConjuntoResponse actualizarConjunto(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarConjuntoRequest request) {
        return conjuntoService.actualizar(id, request);
    }
}
