package safeexit_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ActualizarParametrosRequest(
        @Min(0) @Max(30) int diasAnticipacion) {
}
