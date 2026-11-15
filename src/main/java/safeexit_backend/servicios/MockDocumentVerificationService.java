package safeexit_backend.servicios;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class MockDocumentVerificationService implements DocumentoVerificationService {

    @Override
    public VerificacionDocumentos verificar(
            LocalDate fechaSoat,
            LocalDate fechaRtm,
            LocalDate hoy,
            int diasAnticipacion) {
        EstadoDocumento soat = evaluar(fechaSoat, hoy, diasAnticipacion);
        EstadoDocumento rtm = evaluar(fechaRtm, hoy, diasAnticipacion);
        List<String> motivos = new ArrayList<>();
        if (!"VIGENTE".equals(soat.estado())) {
            motivos.add("SOAT_" + soat.estado());
        }
        if (!"VIGENTE".equals(rtm.estado())) {
            motivos.add("RTM_" + rtm.estado());
        }
        boolean critica = "VENCIDO".equals(soat.estado()) || "VENCIDO".equals(rtm.estado());
        return new VerificacionDocumentos(soat, rtm, !critica, List.copyOf(motivos), critica);
    }

    private EstadoDocumento evaluar(LocalDate vencimiento, LocalDate hoy, int diasAnticipacion) {
        long dias = ChronoUnit.DAYS.between(hoy, vencimiento);
        String estado = dias < 0 ? "VENCIDO"
                : dias <= diasAnticipacion ? "POR_VENCER"
                : "VIGENTE";
        return new EstadoDocumento(vencimiento, dias, estado);
    }
}
