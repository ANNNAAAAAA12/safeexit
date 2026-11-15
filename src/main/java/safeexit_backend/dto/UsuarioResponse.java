package safeexit_backend.dto;

public record UsuarioResponse(
        Long id,
        String nombreCompleto,
        String correo,
        String telefonoWhatsapp,
        String rol,
        boolean activo,
        Long conjuntoId) {
}
