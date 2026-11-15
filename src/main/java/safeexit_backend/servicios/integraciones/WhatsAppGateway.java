package safeexit_backend.servicios.integraciones;

import safeexit_backend.modelos.EstadoEntrega;

public interface WhatsAppGateway {
    ResultadoEnvio enviar(String telefono, String mensaje);

    record ResultadoEnvio(String idProveedor, EstadoEntrega estado) {
    }
}
