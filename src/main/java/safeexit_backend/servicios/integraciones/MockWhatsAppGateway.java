package safeexit_backend.servicios.integraciones;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import safeexit_backend.modelos.EstadoEntrega;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@ConditionalOnProperty(name = "safeexit.integrations.mode", havingValue = "mock", matchIfMissing = true)
public class MockWhatsAppGateway implements WhatsAppGateway {

    private final List<MensajeSimulado> mensajes = new CopyOnWriteArrayList<>();

    @Override
    public ResultadoEnvio enviar(String telefono, String mensaje) {
        mensajes.add(new MensajeSimulado(telefono, mensaje));
        return new ResultadoEnvio("mock-" + System.currentTimeMillis(), EstadoEntrega.ENVIADO);
    }

    public List<MensajeSimulado> obtenerMensajes() {
        return List.copyOf(mensajes);
    }

    public record MensajeSimulado(String telefono, String mensaje) {
    }
}
