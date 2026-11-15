package safeexit_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SimularLecturaRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{3}[0-9]{3}$|^[A-Za-z]{3}[0-9]{2}[A-Za-z]$",
                message = "La placa debe tener el formato colombiano AAA123 o ABC12D")
        String placa) {
}
