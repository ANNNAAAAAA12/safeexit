package safeexit_backend.servicios.integraciones;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import safeexit_backend.modelos.EstadoEntrega;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TwilioWhatsAppGatewayTests {

    @Test
    void sendsConfiguredContentTemplateInsteadOfFreeFormBody() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/AC_TEST/Messages.json"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("To=whatsapp%3A%2B573005296280")))
                .andExpect(content().string(containsString("ContentSid=HX_TEST")))
                .andExpect(content().string(not(containsString("Body="))))
                .andExpect(content().string(containsString("StatusCallback=")))
                .andRespond(withSuccess("{\"sid\":\"SM_TEST\"}", MediaType.APPLICATION_JSON));

        TwilioWhatsAppGateway gateway = new TwilioWhatsAppGateway(
                builder,
                "AC_TEST",
                "test-auth-token",
                "whatsapp:+14155238886",
                "https://safeexit.example.test",
                "HX_TEST");

        var result = gateway.enviar("+573005296280", "Internal alert detail");

        assertEquals("SM_TEST", result.idProveedor());
        assertEquals(EstadoEntrega.ENVIADO, result.estado());
        server.verify();
    }

    @Test
    void sendsWithoutStatusCallbackWhenPublicUrlIsNotConfigured() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/AC_TEST/Messages.json"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(not(containsString("StatusCallback="))))
                .andRespond(withSuccess("{\"sid\":\"SM_TEST\"}", MediaType.APPLICATION_JSON));

        TwilioWhatsAppGateway gateway = new TwilioWhatsAppGateway(
                builder,
                "AC_TEST",
                "test-auth-token",
                "whatsapp:+14155238886",
                "",
                "HX_TEST");

        var result = gateway.enviar("+573005296280", "Internal alert detail");

        assertEquals("SM_TEST", result.idProveedor());
        assertEquals(EstadoEntrega.ENVIADO, result.estado());
        server.verify();
    }

    @Test
    void requiresAnApprovedContentTemplate() {
        assertThrows(IllegalStateException.class, () -> new TwilioWhatsAppGateway(
                RestClient.builder(),
                "AC_TEST",
                "test-auth-token",
                "+14155238886",
                "https://safeexit.example.test",
                ""));
    }
}
