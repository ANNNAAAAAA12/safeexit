package safeexit_backend.servicios;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import safeexit_backend.dto.ProcesarSalidaResponse;
import safeexit_backend.excepciones.RecursoNoEncontradoException;
import safeexit_backend.modelos.*;
import safeexit_backend.repositorios.*;

import java.time.LocalDate;
import java.util.List;

@Service
public class SalidaPersistenceService {

    private final VehiculoRepository vehiculoRepository;
    private final ParametrosSistemaRepository parametrosRepository;
    private final LecturaSalidaRepository lecturaRepository;
    private final EventoAlertaRepository alertaRepository;
    private final RegistroWhatsappRepository whatsappRepository;
    private final DocumentoVerificationService verificacionService;
    private final VehiculoValidacionCache cache;
    private final int diasAnticipacionPredeterminados;
    private final String plantillaAlerta;

    public SalidaPersistenceService(
            VehiculoRepository vehiculoRepository,
            ParametrosSistemaRepository parametrosRepository,
            LecturaSalidaRepository lecturaRepository,
            EventoAlertaRepository alertaRepository,
            RegistroWhatsappRepository whatsappRepository,
            DocumentoVerificationService verificacionService,
            VehiculoValidacionCache cache,
            @Value("${safeexit.alerts.default-advance-days:5}") int diasAnticipacionPredeterminados,
            @Value("${safeexit.notifications.alert-template}") String plantillaAlerta) {
        this.vehiculoRepository = vehiculoRepository;
        this.parametrosRepository = parametrosRepository;
        this.lecturaRepository = lecturaRepository;
        this.alertaRepository = alertaRepository;
        this.whatsappRepository = whatsappRepository;
        this.verificacionService = verificacionService;
        this.cache = cache;
        this.diasAnticipacionPredeterminados = diasAnticipacionPredeterminados;
        if (!plantillaAlerta.contains("{placa}") || !plantillaAlerta.contains("{motivos}")) {
            throw new IllegalStateException("La plantilla de alertas debe incluir {placa} y {motivos}");
        }
        this.plantillaAlerta = plantillaAlerta;
    }

    @Transactional
    public DatosProcesamiento registrarLectura(String placa, byte[] imagen, String tipoContenido) {
        VehiculoValidacionCache.ResumenVehiculo resumen = cache.obtener(placa)
                .orElseGet(() -> vehiculoRepository.findByPlacaIgnoreCase(placa)
                        .map(cache::resumir)
                        .map(valor -> {
                            cache.guardar(valor);
                            return valor;
                        })
                        .orElse(null));
        if (resumen == null) {
            LecturaSalida lectura = lecturaRepository.save(LecturaSalida.builder()
                    .placaDetectada(placa)
                    .documentosAlDia(false)
                    .tipoImagen(tipoContenido)
                    .imagenSalida(imagen)
                    .build());
            return new DatosProcesamiento(lectura.getId(), placa, false, false,
                    List.of(), null, null, null, null);
        }

        if (resumen.fechaVencSoat() == null || resumen.fechaVencRtm() == null) {
            throw new RecursoNoEncontradoException("El vehículo no tiene fechas de documentos registradas");
        }
        int diasAnticipacion = parametrosRepository
                .findByConjuntoId(resumen.conjuntoId())
                .map(ParametrosSistema::getDiasAnticipacion)
                .orElse(diasAnticipacionPredeterminados);
        var verificacion = verificacionService.verificar(
                resumen.fechaVencSoat(), resumen.fechaVencRtm(),
                LocalDate.now(), diasAnticipacion);

        Vehiculo vehiculo = vehiculoRepository.getReferenceById(resumen.vehiculoId());
        LecturaSalida lectura = lecturaRepository.save(LecturaSalida.builder()
                .placaDetectada(placa)
                .vehiculo(vehiculo)
                .documentosAlDia(verificacion.documentosAlDia())
                .tipoImagen(tipoContenido)
                .imagenSalida(imagen)
                .build());
        if (verificacion.motivosAlerta().isEmpty()) {
            return new DatosProcesamiento(lectura.getId(), placa, true, true,
                    List.of(), null, null, null, null);
        }

        EventoAlerta alerta = alertaRepository.save(EventoAlerta.builder()
                .lectura(lectura)
                .motivoAlerta(String.join(",", verificacion.motivosAlerta()))
                .critico(verificacion.critica())
                .build());
        String mensaje = plantillaAlerta
                .replace("{nivel}", verificacion.critica() ? "URGENTE" : "PREVENTIVA")
                .replace("{placa}", placa)
                .replace("{motivos}", String.join(", ", verificacion.motivosAlerta()));
        RegistroWhatsapp notificacion = whatsappRepository.save(RegistroWhatsapp.builder()
                .numeroDestino(resumen.telefono())
                .mensaje(mensaje)
                .estado(EstadoEntrega.PENDIENTE)
                .alerta(alerta)
                .build());
        return new DatosProcesamiento(lectura.getId(), placa, true, verificacion.documentosAlDia(),
                verificacion.motivosAlerta(), alerta.getId(), notificacion.getId(),
                resumen.telefono(), mensaje);
    }

    @Transactional
    public void registrarResultadoEnvio(Long notificacionId, EstadoEntrega estado, String idProveedor) {
        RegistroWhatsapp notificacion = whatsappRepository.findById(notificacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Registro de WhatsApp no encontrado"));
        notificacion.setEstado(estado);
        notificacion.setIdProveedor(idProveedor);
        whatsappRepository.save(notificacion);
    }

    public record DatosProcesamiento(
            Long lecturaId,
            String placa,
            boolean vehiculoRegistrado,
            boolean documentosAlDia,
            List<String> motivos,
            Long alertaId,
            Long notificacionId,
            String telefono,
            String mensaje) {

        public ProcesarSalidaResponse respuesta(String estadoNotificacion) {
            return new ProcesarSalidaResponse(lecturaId, placa, vehiculoRegistrado,
                    documentosAlDia, motivos, estadoNotificacion);
        }
    }
}
