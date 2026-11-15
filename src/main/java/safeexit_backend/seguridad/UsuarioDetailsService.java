package safeexit_backend.seguridad;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import safeexit_backend.modelos.Usuario;
import safeexit_backend.repositorios.UsuarioRepository;

import java.util.Locale;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        String autoridad = usuario.getRol().startsWith("ROLE_")
                ? usuario.getRol()
                : "ROLE_" + usuario.getRol();
        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getContrasena())
                .authorities(new SimpleGrantedAuthority(autoridad))
                .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                .build();
    }
}
