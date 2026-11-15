package safeexit_backend.dto;

import java.time.LocalDateTime;

public record AlertaResponse(
        Long id,
        String placa,
        String motivo,
        boolean critica,
        String estadoEntrega,
        LocalDateTime fecha) {
}
