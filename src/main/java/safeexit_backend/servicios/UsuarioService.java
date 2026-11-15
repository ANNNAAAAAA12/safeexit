package safeexit_backend.servicios;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import safeexit_backend.dto.CrearUsuarioRequest;
import safeexit_backend.dto.UsuarioResponse;
import safeexit_backend.excepciones.ConflictoException;
import safeexit_backend.excepciones.RecursoNoEncontradoException;
import safeexit_backend.modelos.ConjuntoResidencial;
import safeexit_backend.modelos.RolUsuario;
import safeexit_backend.modelos.Usuario;
import safeexit_backend.repositorios.ConjuntoResidencialRepository;
import safeexit_backend.repositorios.UsuarioRepository;
import safeexit_backend.repositorios.VehiculoRepository;

import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ConjuntoResidencialRepository conjuntoRepository;
    private final PasswordEncoder passwordEncoder;
    private final VehiculoRepository vehiculoRepository;
    private final VehiculoValidacionCache vehiculoCache;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            ConjuntoResidencialRepository conjuntoRepository,
            PasswordEncoder passwordEncoder,
            VehiculoRepository vehiculoRepository,
            VehiculoValidacionCache vehiculoCache) {
        this.usuarioRepository = usuarioRepository;
        this.conjuntoRepository = conjuntoRepository;
        this.passwordEncoder = passwordEncoder;
        this.vehiculoRepository = vehiculoRepository;
        this.vehiculoCache = vehiculoCache;
    }

    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request) {
        String correo = request.correo().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictoException("Ya existe una cuenta con ese correo");
        }
        if (request.rol() == RolUsuario.ADMIN) {
            throw new IllegalArgumentException("Las cuentas de administrador se configuran fuera de la API");
        }
        int longitudContrasena = request.contrasena().getBytes(StandardCharsets.UTF_8).length;
        if (longitudContrasena < 12 || longitudContrasena > 72) {
            throw new IllegalArgumentException("La contraseña debe tener entre 12 y 72 bytes UTF-8");
        }
        ConjuntoResidencial conjunto = conjuntoRepository.findById(request.conjuntoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Conjunto residencial no encontrado"));
        Usuario usuario = Usuario.builder()
                .nombreCompleto(request.nombreCompleto().trim())
                .correo(correo)
                .contrasena(passwordEncoder.encode(request.contrasena()))
                .telefonoWhatsapp(request.telefonoWhatsapp())
                .rol(request.rol().autoridad())
                .activo(true)
                .conjunto(conjunto)
                .build();
        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        usuario.setActivo(activo);
        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizarTelefono(String correo, String telefono) {
        Usuario usuario = usuarioRepository.findByCorreo(correo.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        usuario.setTelefonoWhatsapp(telefono);
        vehiculoRepository.findAllByPropietarioIdOrderByPlacaAsc(usuario.getId())
                .forEach(vehiculo -> vehiculoCache.invalidar(vehiculo.getPlaca()));
        return toResponse(usuarioRepository.save(usuario));
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombreCompleto(), usuario.getCorreo(),
                usuario.getTelefonoWhatsapp(), usuario.getRol(), Boolean.TRUE.equals(usuario.getActivo()),
                usuario.getConjunto().getId());
    }
}
