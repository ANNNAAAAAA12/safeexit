package safeexit_backend.dto;

import java.time.LocalDateTime;

public record NotificacionAuditoriaResponse(
        Long id,
        String telefonoDestino,
        String mensaje,
        String estadoEntrega,
        String idProveedor,
        LocalDateTime fechaEnvio) {
}
