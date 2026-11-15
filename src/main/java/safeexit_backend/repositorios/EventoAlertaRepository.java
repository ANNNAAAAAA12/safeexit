package safeexit_backend.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import safeexit_backend.modelos.EventoAlerta;

import java.util.List;

public interface EventoAlertaRepository extends JpaRepository<EventoAlerta, Long> {
    List<EventoAlerta> findTop100ByOrderByFechaCreacionDesc();
    List<EventoAlerta> findTop100ByLecturaVehiculoPropietarioIdOrderByFechaCreacionDesc(Long propietarioId);
}
