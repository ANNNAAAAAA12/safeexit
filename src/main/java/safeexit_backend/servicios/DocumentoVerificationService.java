package safeexit_backend.servicios;

import java.time.LocalDate;
import java.util.List;

public interface DocumentoVerificationService {

    VerificacionDocumentos verificar(LocalDate fechaSoat, LocalDate fechaRtm, LocalDate hoy, int diasAnticipacion);

    record EstadoDocumento(LocalDate vencimiento, long diasRestantes, String estado) {
    }

    record VerificacionDocumentos(
            EstadoDocumento soat,
            EstadoDocumento rtm,
            boolean documentosAlDia,
            List<String> motivosAlerta,
            boolean critica) {
    }
}
