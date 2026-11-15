package safeexit_backend.dto;

public record CacheEstadoResponse(
        boolean habilitado,
        int entradas,
        String proveedor) {
}
