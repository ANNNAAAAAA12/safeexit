package safeexit_backend.controladores;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import safeexit_backend.seguridad.TwilioSignatureValidator;
import safeexit_backend.servicios.NotificacionService;

@RestController
@RequestMapping("/api/v1/webhooks/twilio")
public class TwilioWebhookController {

    private final TwilioSignatureValidator signatureValidator;
    private final NotificacionService notificacionService;

    public TwilioWebhookController(
            TwilioSignatureValidator signatureValidator,
            NotificacionService notificacionService) {
        this.signatureValidator = signatureValidator;
        this.notificacionService = notificacionService;
    }

    @PostMapping(value = "/whatsapp", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> actualizarEstado(
            @RequestHeader(value = "X-Twilio-Signature", required = false) String firma,
            @RequestParam MultiValueMap<String, String> parametros) {
        if (!signatureValidator.validar(firma, parametros)) {
            return ResponseEntity.status(401).build();
        }
        String idMensaje = parametros.getFirst("MessageSid");
        String estado = parametros.getFirst("MessageStatus");
        if (idMensaje == null || idMensaje.isBlank() || estado == null || estado.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        notificacionService.actualizarEstadoProveedor(idMensaje, estado);
        return ResponseEntity.noContent().build();
    }
}
