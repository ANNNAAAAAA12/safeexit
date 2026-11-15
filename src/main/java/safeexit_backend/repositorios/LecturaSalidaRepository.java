package safeexit_backend.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import safeexit_backend.modelos.LecturaSalida;

import java.util.List;
import java.util.Optional;

public interface LecturaSalidaRepository extends JpaRepository<LecturaSalida, Long> {
    List<LecturaSalida> findTop100ByOrderByFechaHoraDesc();
    List<LecturaSalida> findTop100ByVehiculoPropietarioIdOrderByFechaHoraDesc(Long propietarioId);
    Optional<LecturaSalida> findByIdAndVehiculoPropietarioId(Long id, Long propietarioId);
}
