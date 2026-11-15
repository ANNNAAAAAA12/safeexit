package safeexit_backend.dto;

public record VehiculoResponse(
        Long id,
        String placa,
        String tipoVehiculo,
        String marca,
        String modelo,
        EstadoDocumentoResponse soat,
        EstadoDocumentoResponse rtm,
        boolean documentosAlDia) {
}
