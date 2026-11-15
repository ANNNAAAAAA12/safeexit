package safeexit_backend.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoUsuarioRequest(@NotNull Boolean activo) {
}
