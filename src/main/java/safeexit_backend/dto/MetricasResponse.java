package safeexit_backend.dto;

import java.math.BigDecimal;

public record MetricasResponse(
        long vehiculosRegistrados,
        long lecturasProcesadas,
        long alertasGeneradas,
        long mensajesWhatsapp,
        long mensajesEntregados,
        long mensajesFallidos,
        BigDecimal porcentajeEntrega) {
}
