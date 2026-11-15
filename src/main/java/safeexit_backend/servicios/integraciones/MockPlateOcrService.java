package safeexit_backend.servicios.integraciones;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import safeexit_backend.excepciones.ServicioExternoException;

@Service
@ConditionalOnProperty(name = "safeexit.integrations.plate-recognizer.mode", havingValue = "mock", matchIfMissing = true)
public class MockPlateOcrService implements PlateOcrService {

    private final String placaSimulada;

    public MockPlateOcrService(@Value("${safeexit.mock.plate:ABC123}") String placaSimulada) {
        this.placaSimulada = placaSimulada;
    }

    @Override
    public String reconocerPlaca(byte[] imagen, String tipoContenido) {
        if (imagen == null || imagen.length == 0 || !StringUtils.hasText(tipoContenido)
                || !tipoContenido.startsWith("image/")) {
            throw new IllegalArgumentException("Debe enviar una imagen válida");
        }
        return placaSimulada.replaceAll("\\s+", "").toUpperCase();
    }
}
