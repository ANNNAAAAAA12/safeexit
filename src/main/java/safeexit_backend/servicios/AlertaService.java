package safeexit_backend.servicios;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import safeexit_backend.dto.AlertaResponse;
import safeexit_backend.excepciones.RecursoNoEncontradoException;
import safeexit_backend.modelos.EventoAlerta;
import safeexit_backend.modelos.Usuario;
import safeexit_backend.repositorios.EventoAlertaRepository;
import safeexit_backend.repositorios.RegistroWhatsappRepository;
import safeexit_backend.repositorios.UsuarioRepository;

import java.util.List;
import java.util.Locale;

@Service
public class AlertaService {

    private final EventoAlertaRepository alertaRepository;
    private final RegistroWhatsappRepository whatsappRepository;
    private final UsuarioRepository usuarioRepository;

    public AlertaService(
            EventoAlertaRepository alertaRepository,
            RegistroWhatsappRepository whatsappRepository,
            UsuarioRepository usuarioRepository) {
        this.alertaRepository = alertaRepository;
        this.whatsappRepository = whatsappRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<AlertaResponse> historial(String correo, boolean administrador) {
        List<EventoAlerta> alertas;
        if (administrador) {
            alertas = alertaRepository.findTop100ByOrderByFechaCreacionDesc();
        } else {
            Usuario usuario = usuarioRepository.findByCorreo(correo.toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
            alertas = alertaRepository.findTop100ByLecturaVehiculoPropietarioIdOrderByFechaCreacionDesc(
                    usuario.getId());
        }
        return alertas.stream().map(this::mapear).toList();
    }

    private AlertaResponse mapear(EventoAlerta alerta) {
        String estado = whatsappRepository.findFirstByAlertaIdOrderByFechaEnvioDesc(alerta.getId())
                .map(registro -> registro.getEstado().name())
                .orElse("NO_ENVIADA");
        return new AlertaResponse(
                alerta.getId(),
                alerta.getLectura().getPlacaDetectada(),
                alerta.getMotivoAlerta(),
                alerta.isCritico(),
                estado,
                alerta.getFechaCreacion());
    }
}
