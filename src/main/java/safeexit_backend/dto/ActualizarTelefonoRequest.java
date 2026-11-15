package safeexit_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActualizarTelefonoRequest(
        @NotBlank @Pattern(regexp = "^\\+57[0-9]{10}$", message = "Use el formato +57 seguido de 10 dígitos")
        String telefonoWhatsapp) {
}
