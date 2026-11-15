package safeexit_backend.repositorios;

import safeexit_backend.modelos.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    Optional<Vehiculo> findByPlaca(String placa);
    @EntityGraph(attributePaths = {"propietario", "propietario.conjunto", "vencimientoDocumentos"})
    Optional<Vehiculo> findByPlacaIgnoreCase(String placa);
    @EntityGraph(attributePaths = {"propietario", "propietario.conjunto", "vencimientoDocumentos"})
    Optional<Vehiculo> findWithRelationsById(Long id);
    @EntityGraph(attributePaths = {"propietario", "propietario.conjunto", "vencimientoDocumentos"})
    List<Vehiculo> findAllByPropietarioIdOrderByPlacaAsc(Long propietarioId);
    boolean existsByPlacaIgnoreCase(String placa);

    @Override
    @EntityGraph(attributePaths = {"propietario", "vencimientoDocumentos"})
    List<Vehiculo> findAll();

    @Modifying
    @Transactional
    @Query(value = "UPDATE vehiculos SET propietario_id = usuario_id "
            + "WHERE propietario_id IS NULL AND usuario_id IS NOT NULL", nativeQuery = true)
    int migrarPropietariosAnteriores();
}