package safeexit_backend.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class CameraKeyFilter extends OncePerRequestFilter {

    private static final String CAMERA_ENDPOINT = "/api/v1/lecturas/procesar-salida";
    private final String cameraKey;

    public CameraKeyFilter(@Value("${safeexit.security.camera-key:}") String cameraKey) {
        this.cameraKey = cameraKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(request.getContextPath() + CAMERA_ENDPOINT).equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (cameraKey.isBlank()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "La clave de cámara no está configurada");
            return;
        }
        String providedKey = request.getHeader("X-Camera-Key");
        if (providedKey == null || !MessageDigest.isEqual(
                cameraKey.getBytes(StandardCharsets.UTF_8), providedKey.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Clave de cámara inválida");
            return;
        }
        var authentication = new UsernamePasswordAuthenticationToken(
                "camera", null, List.of(new SimpleGrantedAuthority("ROLE_CAMERA")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
