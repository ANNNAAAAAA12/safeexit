package safeexit_backend.servicios;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import safeexit_backend.dto.LecturaAuditoriaResponse;
import safeexit_backend.dto.NotificacionAuditoriaResponse;
import safeexit_backend.excepciones.RecursoNoEncontradoException;
import safeexit_backend.modelos.LecturaSalida;
import safeexit_backend.modelos.RegistroWhatsapp;
import safeexit_backend.repositorios.LecturaSalidaRepository;
import safeexit_backend.repositorios.RegistroWhatsappRepository;

import java.util.List;

@Service
public class AuditoriaService {

    private final LecturaSalidaRepository lecturaRepository;
    private final RegistroWhatsappRepository whatsappRepository;

    public AuditoriaService(
            LecturaSalidaRepository lecturaRepository,
            RegistroWhatsappRepository whatsappRepository) {
        this.lecturaRepository = lecturaRepository;
        this.whatsappRepository = whatsappRepository;
    }

    @Transactional(readOnly = true)
    public List<LecturaAuditoriaResponse> listarLecturas() {
        return lecturaRepository.findTop100ByOrderByFechaHoraDesc().stream()
                .map(this::mapearLectura).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificacionAuditoriaResponse> listarNotificaciones() {
        return whatsappRepository.findTop100ByOrderByFechaEnvioDesc().stream()
                .map(this::mapearNotificacion).toList();
    }

    @Transactional(readOnly = true)
    public ImagenAuditoria obtenerImagen(Long lecturaId) {
        LecturaSalida lectura = lecturaRepository.findById(lecturaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lectura de salida no encontrada"));
        if (lectura.getImagenSalida() == null || lectura.getImagenSalida().length == 0) {
            throw new RecursoNoEncontradoException("La lectura no tiene una imagen almacenada");
        }
        return new ImagenAuditoria(lectura.getImagenSalida(), lectura.getTipoImagen());
    }

    private LecturaAuditoriaResponse mapearLectura(LecturaSalida lectura) {
        return new LecturaAuditoriaResponse(lectura.getId(), lectura.getPlacaDetectada(),
                lectura.getVehiculo() == null ? null : lectura.getVehiculo().getId(),
                Boolean.TRUE.equals(lectura.getDocumentosAlDia()), lectura.getFechaHora(),
                lectura.getImagenSalida() != null && lectura.getImagenSalida().length > 0);
    }

    private NotificacionAuditoriaResponse mapearNotificacion(RegistroWhatsapp registro) {
        return new NotificacionAuditoriaResponse(registro.getId(), registro.getNumeroDestino(),
                registro.getMensaje(), registro.getEstado().name(), registro.getIdProveedor(),
                registro.getFechaEnvio());
    }

    public record ImagenAuditoria(byte[] bytes, String tipoContenido) {
    }
}
