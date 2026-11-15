package safeexit_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import safeexit_backend.modelos.RolUsuario;

public record CrearUsuarioRequest(
        @NotBlank @Size(max = 100) String nombreCompleto,
        @NotBlank @Email @Size(max = 100) String correo,
        @NotBlank @Size(min = 12, max = 72) String contrasena,
        @NotBlank @Pattern(regexp = "^\\+57[0-9]{10}$", message = "Use el formato colombiano +57 seguido de 10 dígitos") String telefonoWhatsapp,
        @NotNull RolUsuario rol,
        @NotNull Long conjuntoId) {
}
