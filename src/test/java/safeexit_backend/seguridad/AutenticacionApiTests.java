package safeexit_backend.seguridad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AutenticacionApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void protectedAdminRouteRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/metricas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanLoginAndReadProtectedMetrics() throws Exception {
        String token = login("admin@test.safeexit", "test-admin-password-2026");

        mockMvc.perform(get("/api/v1/admin/metricas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajesWhatsapp").isNumber());

        long conjuntoId = objectMapper.readTree(mockMvc.perform(get("/api/v1/admin/conjuntos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).get(0).path("id").asLong();
        mockMvc.perform(get("/api/v1/admin/conjuntos/" + conjuntoId + "/parametros")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diasAnticipacion").value(5));
    }

    @Test
    void residentCanRegisterVehicleAndReceivePreventiveAlertFromCameraFlow() throws Exception {
        String adminToken = login("admin@test.safeexit", "test-admin-password-2026");
        long conjuntoId = objectMapper.readTree(mockMvc.perform(get("/api/v1/admin/conjuntos")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).get(0).path("id").asLong();
        String residentResponse = mockMvc.perform(post("/api/v1/admin/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nombreCompleto", "Residente de Prueba",
                                "correo", "residente@test.safeexit",
                                "contrasena", "resident-test-password-2026",
                                "telefonoWhatsapp", "+573001234568",
                                "rol", "RESIDENT",
                                "conjuntoId", conjuntoId))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(conjuntoId,
                objectMapper.readTree(residentResponse).path("conjuntoId").asLong());
        String residentToken = login("residente@test.safeexit", "resident-test-password-2026");

        mockMvc.perform(post("/api/v1/vehiculos")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "placa", "ABC123",
                                "tipoVehiculo", "CARRO",
                                "marca", "Mazda",
                                "modelo", "CX-30",
                                "fechaVencSoat", LocalDate.now().minusDays(1).toString(),
                                "fechaVencRtm", LocalDate.now().plusDays(30).toString()))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/vehiculos")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "placa", "XYZ987",
                                "tipoVehiculo", "MOTO",
                                "marca", "Honda",
                                "modelo", "CB"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.soat.fechaVencimiento").isNotEmpty())
                .andExpect(jsonPath("$.rtm.fechaVencimiento").isNotEmpty());

        mockMvc.perform(get("/api/v1/admin/demo/consulta-documentos/XYZ987")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placa").value("XYZ987"))
                .andExpect(jsonPath("$.fuenteConsulta").value("SIMULADA"))
                .andExpect(jsonPath("$.soat.fechaVencimiento").isNotEmpty())
                .andExpect(jsonPath("$.rtm.fechaVencimiento").isNotEmpty());

        mockMvc.perform(post("/api/v1/vehiculos")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "placa", "LMN456",
                                "tipoVehiculo", "CARRO",
                                "fechaVencSoat", LocalDate.now().plusDays(30).toString()))))
                .andExpect(status().isBadRequest());

        MockMultipartFile imagen = new MockMultipartFile(
                "imagen", "salida.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01});
        mockMvc.perform(multipart("/api/v1/lecturas/procesar-salida")
                        .file(imagen)
                        .header("X-Camera-Key", "test-camera-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placaDetectada").value("ABC123"))
                .andExpect(jsonPath("$.vehiculoRegistrado").value(true))
                .andExpect(jsonPath("$.documentosAlDia").value(false))
                .andExpect(jsonPath("$.estadoNotificacion").value("ENVIADO"));

        mockMvc.perform(get("/api/v1/alertas/historial")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].motivo").value("SOAT_VENCIDO"));
        mockMvc.perform(get("/api/v1/auditoria/lecturas")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].imagenDisponible").value(true));
        mockMvc.perform(get("/api/v1/auditoria/cache")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitado").value(true));

        String idProveedor = objectMapper.readTree(mockMvc.perform(get("/api/v1/auditoria/notificaciones")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).get(0).path("idProveedor").asText();
        MultiValueMap<String, String> callback = new LinkedMultiValueMap<>();
        callback.add("MessageSid", idProveedor);
        callback.add("MessageStatus", "delivered");
        mockMvc.perform(post("/api/v1/webhooks/twilio/whatsapp")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(callback)
                        .header("X-Twilio-Signature", firmarCallback(callback)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/admin/metricas")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajesEntregados").value(1))
                .andExpect(jsonPath("$.porcentajeEntrega").value(100.0));

        mockMvc.perform(post("/api/v1/admin/demo/lecturas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("placa", "ABC123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehiculoRegistrado").value(true))
                .andExpect(jsonPath("$.placaDetectada").value("ABC123"))
                .andExpect(jsonPath("$.soat.estado").value("VENCIDO"))
                .andExpect(jsonPath("$.motivosAlerta[0]").value("SOAT_VENCIDO"))
                .andExpect(jsonPath("$.estadoNotificacion").value("ENVIADO"));

        mockMvc.perform(get("/api/v1/admin/metricas")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lecturasProcesadas").value(2))
                .andExpect(jsonPath("$.alertasGeneradas").value(2))
                .andExpect(jsonPath("$.mensajesWhatsapp").value(2))
                .andExpect(jsonPath("$.mensajesEntregados").value(1))
                .andExpect(jsonPath("$.porcentajeEntrega").value(50.0));
    }

    private String login(String correo, String contrasena) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "correo", correo,
                                "contrasena", contrasena))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode tokenResponse = objectMapper.readTree(response);
        return tokenResponse.path("token").asText();
    }

    private String firmarCallback(MultiValueMap<String, String> parametros) throws Exception {
        StringBuilder contenido = new StringBuilder(
                "https://safeexit.example.test/api/v1/webhooks/twilio/whatsapp");
        new TreeMap<>(parametros).forEach((nombre, valores) ->
                valores.stream().sorted().forEach(valor -> contenido.append(nombre).append(valor)));
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec("test-twilio-auth-token".getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(contenido.toString().getBytes(StandardCharsets.UTF_8)));
    }
}
