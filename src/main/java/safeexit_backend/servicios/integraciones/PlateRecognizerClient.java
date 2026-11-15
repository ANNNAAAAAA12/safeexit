package safeexit_backend.servicios.integraciones;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import safeexit_backend.excepciones.ServicioExternoException;

@Service
@ConditionalOnProperty(name = "safeexit.integrations.mode", havingValue = "real")
public class PlateRecognizerClient implements PlateOcrService {

    private final RestClient restClient;
    private final String apiToken;

    public PlateRecognizerClient(
            RestClient.Builder restClientBuilder,
            @Value("${safeexit.integrations.plate-recognizer.token}") String apiToken) {
        if (apiToken.isBlank()) {
            throw new IllegalStateException("Configure PLATE_RECOGNIZER_TOKEN para usar OCR real");
        }
        this.restClient = restClientBuilder
                .baseUrl("https://api.platerecognizer.com/v1")
                .build();
        this.apiToken = apiToken;
    }

    @Override
    public String reconocerPlaca(byte[] imagen, String tipoContenido) {
        ByteArrayResource resource = new ByteArrayResource(imagen) {
            @Override
            public String getFilename() {
                return "captura";
            }
        };
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.parseMediaType(tipoContenido));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("upload", new HttpEntity<>(resource, partHeaders));
        try {
            JsonNode response = restClient.post()
                    .uri("/plate-reader/")
                    .header(HttpHeaders.AUTHORIZATION, "Token " + apiToken)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode results = response == null ? null : response.path("results");
            if (results == null || !results.isArray() || results.isEmpty()) {
                throw new ServicioExternoException("Plate Recognizer no detectó una placa");
            }
            String plate = results.get(0).path("plate").asText("");
            if (plate.isBlank()) {
                throw new ServicioExternoException("Plate Recognizer devolvió una lectura vacía");
            }
            return plate.replaceAll("\\s+", "").toUpperCase();
        } catch (RestClientException exception) {
            throw new ServicioExternoException("No fue posible consultar Plate Recognizer", exception);
        }
    }
}
