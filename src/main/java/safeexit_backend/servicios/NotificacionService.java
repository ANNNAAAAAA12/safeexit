package safeexit_backend.servicios;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import safeexit_backend.excepciones.RecursoNoEncontradoException;
import safeexit_backend.excepciones.ServicioExternoException;
import safeexit_backend.modelos.EstadoEntrega;
import safeexit_backend.modelos.RegistroWhatsapp;
import safeexit_backend.modelos.Usuario;
import safeexit_backend.repositorios.RegistroWhatsappRepository;
import safeexit_backend.repositorios.UsuarioRepository;
import safeexit_backend.servicios.integraciones.WhatsAppGateway;

import java.util.Locale;

@Service
public class NotificacionService {

    private final UsuarioRepository usuarioRepository;
    private final RegistroWhatsappRepository whatsappRepository;
    private final WhatsAppGateway whatsappGateway;

    public NotificacionService(
            UsuarioRepository usuarioRepository,
            RegistroWhatsappRepository whatsappRepository,
            WhatsAppGateway whatsappGateway) {
        this.usuarioRepository = usuarioRepository;
        this.whatsappRepository = whatsappRepository;
        this.whatsappGateway = whatsappGateway;
    }

    public EstadoEntrega enviarPrueba(String correo, String mensaje) {
        Usuario usuario = usuarioRepository.findByCorreo(correo.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        RegistroWhatsapp registro = guardarRegistro(usuario.getTelefonoWhatsapp(), mensaje);
        try {
            var resultado = whatsappGateway.enviar(usuario.getTelefonoWhatsapp(), mensaje);
            guardarResultado(registro.getId(), resultado.estado(), resultado.idProveedor());
            return resultado.estado();
        } catch (ServicioExternoException exception) {
            guardarResultado(registro.getId(), EstadoEntrega.FALLIDO, null);
            throw exception;
        }
    }

    @Transactional
    public void actualizarEstadoProveedor(String idProveedor, String estadoProveedor) {
        RegistroWhatsapp registro = whatsappRepository.findByIdProveedor(idProveedor)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje de WhatsApp no encontrado"));
        EstadoEntrega nuevoEstado = switch (estadoProveedor.toLowerCase(Locale.ROOT)) {
            case "delivered", "read" -> EstadoEntrega.ENTREGADO;
            case "failed", "undelivered", "canceled" -> EstadoEntrega.FALLIDO;
            case "sent" -> EstadoEntrega.ENVIADO;
            case "accepted", "queued", "sending", "scheduled" -> EstadoEntrega.PENDIENTE;
            default -> throw new IllegalArgumentException("Estado de Twilio no reconocido");
        };
        if (registro.getEstado() == EstadoEntrega.ENTREGADO || registro.getEstado() == EstadoEntrega.FALLIDO) {
            return;
        }
        if (nuevoEstado != EstadoEntrega.PENDIENTE || registro.getEstado() == EstadoEntrega.PENDIENTE) {
            registro.setEstado(nuevoEstado);
            whatsappRepository.save(registro);
        }
    }

    @Transactional
    protected RegistroWhatsapp guardarRegistro(String telefono, String mensaje) {
        return whatsappRepository.save(RegistroWhatsapp.builder()
                .numeroDestino(telefono)
                .mensaje(mensaje)
                .estado(EstadoEntrega.PENDIENTE)
                .build());
    }

    @Transactional
    protected void guardarResultado(Long id, EstadoEntrega estado, String idProveedor) {
        RegistroWhatsapp registro = whatsappRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Registro de WhatsApp no encontrado"));
        registro.setEstado(estado);
        registro.setIdProveedor(idProveedor);
        whatsappRepository.save(registro);
    }
}
