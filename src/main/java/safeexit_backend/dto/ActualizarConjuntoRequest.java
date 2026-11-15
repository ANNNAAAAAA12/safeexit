package safeexit_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarConjuntoRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 150) String direccion,
        @NotBlank @Size(max = 30) String nit) {
}
