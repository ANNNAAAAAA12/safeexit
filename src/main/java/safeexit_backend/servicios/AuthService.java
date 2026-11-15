package safeexit_backend.servicios;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import safeexit_backend.dto.LoginRequest;
import safeexit_backend.dto.TokenResponse;
import safeexit_backend.seguridad.JwtService;
import safeexit_backend.seguridad.UsuarioDetailsService;

import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioDetailsService usuarioDetailsService;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UsuarioDetailsService usuarioDetailsService,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioDetailsService = usuarioDetailsService;
        this.jwtService = jwtService;
    }

    public TokenResponse iniciarSesion(LoginRequest request) {
        String correo = request.correo().toLowerCase(Locale.ROOT);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(correo, request.contrasena()));
        UserDetails usuario = usuarioDetailsService.loadUserByUsername(correo);
        List<String> roles = usuario.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();
        return new TokenResponse(jwtService.generarToken(usuario), "Bearer",
                jwtService.getExpirationSeconds(), roles);
    }
}
