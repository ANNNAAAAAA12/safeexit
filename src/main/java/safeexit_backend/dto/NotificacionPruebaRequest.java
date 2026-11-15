package safeexit_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificacionPruebaRequest(
        @NotBlank @Size(max = 500) String mensaje) {
}
