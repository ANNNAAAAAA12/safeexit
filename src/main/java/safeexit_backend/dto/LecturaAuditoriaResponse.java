package safeexit_backend.dto;

import java.time.LocalDateTime;

public record LecturaAuditoriaResponse(
        Long id,
        String placaDetectada,
        Long vehiculoId,
        boolean documentosAlDia,
        LocalDateTime fechaHora,
        boolean imagenDisponible) {
}
