package safeexit_backend.servicios.integraciones;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import safeexit_backend.excepciones.ServicioExternoException;
import safeexit_backend.modelos.EstadoEntrega;

@Service
@ConditionalOnProperty(name = "safeexit.integrations.whatsapp-mode", havingValue = "real")
public class TwilioWhatsAppGateway implements WhatsAppGateway {

    private final RestClient restClient;
    private final String accountSid;
    private final String authToken;
    private final String sender;
    private final String publicUrl;
    private final String contentSid;

    public TwilioWhatsAppGateway(
            RestClient.Builder restClientBuilder,
            @Value("${safeexit.integrations.twilio.account-sid}") String accountSid,
            @Value("${safeexit.integrations.twilio.auth-token}") String authToken,
            @Value("${safeexit.integrations.twilio.sender}") String sender,
            @Value("${safeexit.integrations.twilio.public-url:}") String publicUrl,
            @Value("${safeexit.integrations.twilio.content-sid:}") String contentSid) {
        if (accountSid.isBlank() || authToken.isBlank() || sender.isBlank() || contentSid.isBlank()) {
            throw new IllegalStateException(
                    "Configure TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_WHATSAPP_SENDER y "
                            + "TWILIO_WHATSAPP_CONTENT_SID");
        }
        this.restClient = restClientBuilder
                .baseUrl("https://api.twilio.com/2010-04-01/Accounts/" + accountSid)
                .build();
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.sender = sender;
        this.publicUrl = publicUrl == null ? "" : publicUrl.replaceAll("/+$", "");
        this.contentSid = contentSid == null ? "" : contentSid.trim();
    }

    @Override
    public ResultadoEnvio enviar(String telefono, String mensaje) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("From", normalizarNumeroWhatsApp(sender));
        body.add("To", normalizarNumeroWhatsApp(telefono));
        body.add("ContentSid", contentSid);
        if (!publicUrl.isBlank()) {
            body.add("StatusCallback", publicUrl + "/api/v1/webhooks/twilio/whatsapp");
        }
        try {
            JsonNode response = restClient.post()
                    .uri("/Messages.json")
                    .headers(headers -> headers.setBasicAuth(accountSid, authToken))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String sid = response == null ? "" : response.path("sid").asText("");
            if (sid.isBlank()) {
                throw new ServicioExternoException("Twilio no devolvió el identificador del mensaje");
            }
            return new ResultadoEnvio(sid, EstadoEntrega.ENVIADO);
        } catch (RestClientException exception) {
            throw new ServicioExternoException("No fue posible enviar el mensaje por Twilio", exception);
        }
    }

    private String normalizarNumeroWhatsApp(String numero) {
        return numero.startsWith("whatsapp:") ? numero : "whatsapp:" + numero;
    }
}
