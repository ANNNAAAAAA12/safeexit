package safeexit_backend.excepciones;

import java.time.Instant;

public record RespuestaError(
        Instant fecha,
        int estado,
        String error,
        String mensaje,
        String ruta) {
}
