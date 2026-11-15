package safeexit_backend.dto;

import java.util.List;

public record ProcesarSalidaResponse(
        Long lecturaId,
        String placaDetectada,
        boolean vehiculoRegistrado,
        boolean documentosAlDia,
        List<String> motivosAlerta,
        String estadoNotificacion) {
}
