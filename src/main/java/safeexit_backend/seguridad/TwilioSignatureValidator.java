package safeexit_backend.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

@Component
public class TwilioSignatureValidator {

    private final String authToken;
    private final String publicUrl;

    public TwilioSignatureValidator(
            @Value("${safeexit.integrations.twilio.auth-token:}") String authToken,
            @Value("${safeexit.integrations.twilio.public-url:}") String publicUrl) {
        this.authToken = authToken;
        this.publicUrl = publicUrl == null ? "" : publicUrl.replaceAll("/+$", "");
    }

    public boolean validar(String firma, MultiValueMap<String, String> parametros) {
        if (firma == null || authToken.isBlank() || publicUrl.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(authToken.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] expected = mac.doFinal(construirMensaje(parametros).getBytes(StandardCharsets.UTF_8));
            byte[] provided = Base64.getDecoder().decode(firma);
            return MessageDigest.isEqual(expected, provided);
        } catch (java.security.GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    private String construirMensaje(MultiValueMap<String, String> parametros) {
        StringBuilder mensaje = new StringBuilder(publicUrl)
                .append("/api/v1/webhooks/twilio/whatsapp");
        Map<String, java.util.List<String>> ordenados = new TreeMap<>(parametros);
        ordenados.forEach((nombre, valores) -> valores.stream().sorted()
                .forEach(valor -> mensaje.append(nombre).append(valor)));
        return mensaje.toString();
    }
}
