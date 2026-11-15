package safeexit_backend.dto;

public record ConsultaDocumentosResponse(
        String placa,
        String fuenteConsulta,
        EstadoDocumentoResponse soat,
        EstadoDocumentoResponse rtm,
        boolean documentosAlDia) {
}
