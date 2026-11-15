package safeexit_backend.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import safeexit_backend.modelos.RegistroWhatsapp;
import safeexit_backend.modelos.EstadoEntrega;

import java.util.List;

public interface RegistroWhatsappRepository extends JpaRepository<RegistroWhatsapp, Long> {
    List<RegistroWhatsapp> findTop100ByOrderByFechaEnvioDesc();
    List<RegistroWhatsapp> findTop100ByAlertaLecturaVehiculoPropietarioIdOrderByFechaEnvioDesc(Long propietarioId);
    java.util.Optional<RegistroWhatsapp> findFirstByAlertaIdOrderByFechaEnvioDesc(Long alertaId);
    java.util.Optional<RegistroWhatsapp> findByIdProveedor(String idProveedor);
    long countByEstado(EstadoEntrega estado);
}
