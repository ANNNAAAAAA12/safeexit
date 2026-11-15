package safeexit_backend.servicios;

import org.springframework.stereotype.Service;
import safeexit_backend.dto.MetricasResponse;
import safeexit_backend.modelos.EstadoEntrega;
import safeexit_backend.repositorios.EventoAlertaRepository;
import safeexit_backend.repositorios.LecturaSalidaRepository;
import safeexit_backend.repositorios.RegistroWhatsappRepository;
import safeexit_backend.repositorios.VehiculoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class MetricasService {

    private final VehiculoRepository vehiculoRepository;
    private final LecturaSalidaRepository lecturaRepository;
    private final EventoAlertaRepository alertaRepository;
    private final RegistroWhatsappRepository whatsappRepository;

    public MetricasService(
            VehiculoRepository vehiculoRepository,
            LecturaSalidaRepository lecturaRepository,
            EventoAlertaRepository alertaRepository,
            RegistroWhatsappRepository whatsappRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.lecturaRepository = lecturaRepository;
        this.alertaRepository = alertaRepository;
        this.whatsappRepository = whatsappRepository;
    }

    public MetricasResponse obtener() {
        long totalMensajes = whatsappRepository.count();
        long entregados = whatsappRepository.countByEstado(EstadoEntrega.ENTREGADO);
        long fallidos = whatsappRepository.countByEstado(EstadoEntrega.FALLIDO);
        BigDecimal porcentaje = totalMensajes == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(entregados)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalMensajes), 2, RoundingMode.HALF_UP);
        return new MetricasResponse(vehiculoRepository.count(), lecturaRepository.count(),
                alertaRepository.count(), totalMensajes, entregados, fallidos, porcentaje);
    }
}
