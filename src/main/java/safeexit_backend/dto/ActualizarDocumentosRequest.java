package safeexit_backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ActualizarDocumentosRequest(
        @NotNull LocalDate fechaVencSoat,
        @NotNull LocalDate fechaVencRtm) {
}
