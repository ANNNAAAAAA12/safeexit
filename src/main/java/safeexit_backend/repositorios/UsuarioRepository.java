package safeexit_backend.repositorios;

import safeexit_backend.modelos.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);

    @Override
    @EntityGraph(attributePaths = "conjunto")
    List<Usuario> findAll();
}