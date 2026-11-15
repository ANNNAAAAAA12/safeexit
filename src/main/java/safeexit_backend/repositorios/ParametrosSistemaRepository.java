package safeexit_backend.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import safeexit_backend.modelos.ParametrosSistema;

import java.util.Optional;

public interface ParametrosSistemaRepository extends JpaRepository<ParametrosSistema, Long> {
    Optional<ParametrosSistema> findByConjuntoId(Long conjuntoId);
}
