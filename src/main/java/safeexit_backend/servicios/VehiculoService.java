package safeexit_backend.servicios;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import safeexit_backend.dto.CrearVehiculoRequest;
import safeexit_backend.dto.EstadoDocumentoResponse;
import safeexit_backend.dto.ActualizarDocumentosRequest;
import safeexit_backend.dto.ConsultaDocumentosResponse;
import safeexit_backend.dto.VehiculoResponse;
import safeexit_backend.excepciones.ConflictoException;
import safeexit_backend.excepciones.RecursoNoEncontradoException;
import safeexit_backend.modelos.ParametrosSistema;
import safeexit_backend.modelos.Usuario;
import safeexit_backend.modelos.Vehiculo;
import safeexit_backend.modelos.VencimientoDocumentos;
import safeexit_backend.repositorios.ParametrosSistemaRepository;
import safeexit_backend.repositorios.UsuarioRepository;
import safeexit_backend.repositorios.VehiculoRepository;
import safeexit_backend.servicios.integraciones.VehicleDocumentsLookup;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class VehiculoService {

    private static final Pattern PLACA_COLOMBIANA =
            Pattern.compile("^[A-Z]{3}[0-9]{3}$|^[A-Z]{3}[0-9]{2}[A-Z]$");

    private final VehiculoRepository vehiculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ParametrosSistemaRepository parametrosRepository;
    private final DocumentoVerificationService verificacionService;
    private final VehiculoValidacionCache cache;
    private final VehicleDocumentsLookup documentsLookup;

    public VehiculoService(
            VehiculoRepository vehiculoRepository,
            UsuarioRepository usuarioRepository,
            ParametrosSistemaRepository parametrosRepository,
            DocumentoVerificationService verificacionService,
            VehiculoValidacionCache cache,
            VehicleDocumentsLookup documentsLookup) {
        this.vehiculoRepository = vehiculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.parametrosRepository = parametrosRepository;
        this.verificacionService = verificacionService;
        this.cache = cache;
        this.documentsLookup = documentsLookup;
    }

    @Transactional
    public VehiculoResponse crear(String correo, CrearVehiculoRequest request) {
        Usuario propietario = usuarioRepository.findByCorreo(correo.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        String placa = normalizarPlaca(request.placa());
        if (vehiculoRepository.existsByPlacaIgnoreCase(placa)) {
            throw new ConflictoException("Ya existe un vehículo registrado con esa placa");
        }

        Vehiculo vehiculo = Vehiculo.builder()
                .placa(placa)
                .tipoVehiculo(request.tipoVehiculo())
                .marca(limpiar(request.marca()))
                .modelo(limpiar(request.modelo()))
                .propietario(propietario)
                .build();
        VehicleDocumentsLookup.DocumentDates fechas = request.fechaVencSoat() == null
                ? documentsLookup.consultar(placa)
                : new VehicleDocumentsLookup.DocumentDates(request.fechaVencSoat(), request.fechaVencRtm());
        VencimientoDocumentos documentos = VencimientoDocumentos.builder()
                .vehiculo(vehiculo)
                .fechaVencSoat(fechas.fechaVencSoat())
                .fechaVencRtm(fechas.fechaVencRtm())
                .build();
        vehiculo.setVencimientoDocumentos(documentos);
        Vehiculo guardado = vehiculoRepository.save(vehiculo);
        cache.invalidar(placa);
        return toResponse(guardado);
    }

    @Transactional(readOnly = true)
    public List<VehiculoResponse> listarPropios(String correo) {
        Usuario propietario = usuarioRepository.findByCorreo(correo.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return vehiculoRepository.findAllByPropietarioIdOrderByPlacaAsc(propietario.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ConsultaDocumentosResponse consultarDocumentosDemo(String correo, String placaRecibida) {
        String placa = normalizarPlaca(placaRecibida);
        if (!PLACA_COLOMBIANA.matcher(placa).matches()) {
            throw new IllegalArgumentException("La placa debe tener el formato colombiano AAA123 o ABC12D");
        }
        Usuario administrador = usuarioRepository.findByCorreo(correo.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        int diasAnticipacion = parametrosRepository.findByConjuntoId(administrador.getConjunto().getId())
                .map(ParametrosSistema::getDiasAnticipacion).orElse(5);
        var fechas = documentsLookup.consultar(placa);
        var verificacion = verificacionService.verificar(
                fechas.fechaVencSoat(), fechas.fechaVencRtm(), LocalDate.now(), diasAnticipacion);
        return new ConsultaDocumentosResponse(
                placa,
                "SIMULADA",
                mapearEstado(verificacion.soat()),
                mapearEstado(verificacion.rtm()),
                verificacion.documentosAlDia());
    }

    @Transactional
    public VehiculoResponse actualizarDocumentos(
            String correo, Long vehiculoId, ActualizarDocumentosRequest request, boolean administrador) {
        Vehiculo vehiculo = vehiculoRepository.findWithRelationsById(vehiculoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo no encontrado"));
        if (!administrador && !vehiculo.getPropietario().getCorreo().equalsIgnoreCase(correo)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No puede modificar documentos de otro residente");
        }
        VencimientoDocumentos documentos = vehiculo.getVencimientoDocumentos();
        if (documentos == null) {
            documentos = VencimientoDocumentos.builder().vehiculo(vehiculo).build();
            vehiculo.setVencimientoDocumentos(documentos);
        }
        documentos.setFechaVencSoat(request.fechaVencSoat());
        documentos.setFechaVencRtm(request.fechaVencRtm());
        Vehiculo guardado = vehiculoRepository.save(vehiculo);
        cache.invalidar(vehiculo.getPlaca());
        return toResponse(guardado);
    }

    private VehiculoResponse toResponse(Vehiculo vehiculo) {
        VencimientoDocumentos documentos = vehiculo.getVencimientoDocumentos();
        int diasAnticipacion = parametrosRepository.findByConjuntoId(vehiculo.getPropietario().getConjunto().getId())
                .map(ParametrosSistema::getDiasAnticipacion).orElse(5);
        var verificacion = verificacionService.verificar(documentos.getFechaVencSoat(),
                documentos.getFechaVencRtm(), LocalDate.now(), diasAnticipacion);
        return new VehiculoResponse(
                vehiculo.getId(),
                vehiculo.getPlaca(),
                vehiculo.getTipoVehiculo().name(),
                vehiculo.getMarca(),
                vehiculo.getModelo(),
                mapearEstado(verificacion.soat()),
                mapearEstado(verificacion.rtm()),
                verificacion.documentosAlDia());
    }

    private EstadoDocumentoResponse mapearEstado(DocumentoVerificationService.EstadoDocumento estado) {
        return new EstadoDocumentoResponse(estado.vencimiento(), estado.diasRestantes(), estado.estado());
    }

    private String normalizarPlaca(String placa) {
        return placa.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
