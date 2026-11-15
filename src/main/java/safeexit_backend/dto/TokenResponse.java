package safeexit_backend.dto;

import java.util.List;

public record TokenResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        List<String> roles) {
}
