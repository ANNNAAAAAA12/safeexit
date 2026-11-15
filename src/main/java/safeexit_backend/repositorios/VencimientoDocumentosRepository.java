package safeexit_backend.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import safeexit_backend.modelos.VencimientoDocumentos;

import java.util.Optional;

public interface VencimientoDocumentosRepository extends JpaRepository<VencimientoDocumentos, Long> {
    Optional<VencimientoDocumentos> findByVehiculoId(Long vehiculoId);
}
