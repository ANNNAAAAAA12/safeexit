package safeexit_backend.dto;

import java.time.LocalDate;

public record EstadoDocumentoResponse(
        LocalDate fechaVencimiento,
        long diasRestantes,
        String estado) {
}
