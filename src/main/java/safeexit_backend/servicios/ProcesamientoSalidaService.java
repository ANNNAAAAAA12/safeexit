package safeexit_backend.servicios;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import safeexit_backend.dto.ProcesarSalidaResponse;
import safeexit_backend.excepciones.ServicioExternoException;
import safeexit_backend.modelos.EstadoEntrega;
import safeexit_backend.servicios.integraciones.PlateOcrService;
import safeexit_backend.servicios.integraciones.WhatsAppGateway;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ProcesamientoSalidaService {

    private static final Pattern PLACA_COLOMBIANA =
            Pattern.compile("^[A-Z]{3}[0-9]{3}$|^[A-Z]{3}[0-9]{2}[A-Z]$");

    private final PlateOcrService plateOcrService;
    private final WhatsAppGateway whatsappGateway;
    private final SalidaPersistenceService persistenceService;

    public ProcesamientoSalidaService(
            PlateOcrService plateOcrService,
            WhatsAppGateway whatsappGateway,
            SalidaPersistenceService persistenceService) {
        this.plateOcrService = plateOcrService;
        this.whatsappGateway = whatsappGateway;
        this.persistenceService = persistenceService;
    }

    public ProcesarSalidaResponse procesar(byte[] imagen, String tipoContenido) {
        validarImagen(imagen, tipoContenido);
        String placa = plateOcrService.reconocerPlaca(imagen, tipoContenido)
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
        if (!PLACA_COLOMBIANA.matcher(placa).matches()) {
            throw new ServicioExternoException("El servicio OCR no devolvió una placa colombiana válida");
        }

        SalidaPersistenceService.DatosProcesamiento datos =
                persistenceService.registrarLectura(placa, imagen, tipoContenido);
        if (datos.notificacionId() == null) {
            return datos.respuesta("NO_REQUERIDA");
        }
        try {
            var resultado = whatsappGateway.enviar(datos.telefono(), datos.mensaje());
            persistenceService.registrarResultadoEnvio(
                    datos.notificacionId(), resultado.estado(), resultado.idProveedor());
            return datos.respuesta(resultado.estado().name());
        } catch (ServicioExternoException exception) {
            persistenceService.registrarResultadoEnvio(datos.notificacionId(), EstadoEntrega.FALLIDO, null);
            throw exception;
        }
    }

    private void validarImagen(byte[] imagen, String tipoContenido) {
        if (imagen == null || imagen.length == 0) {
            throw new IllegalArgumentException("La imagen es obligatoria");
        }
        if (imagen.length > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("La imagen supera el límite de 5 MB");
        }
        if (!StringUtils.hasText(tipoContenido)
                || !(tipoContenido.equalsIgnoreCase("image/jpeg") || tipoContenido.equalsIgnoreCase("image/png"))) {
            throw new IllegalArgumentException("Solo se aceptan imágenes JPEG o PNG");
        }
        boolean jpeg = imagen.length >= 3 && (imagen[0] & 0xFF) == 0xFF
                && (imagen[1] & 0xFF) == 0xD8 && (imagen[2] & 0xFF) == 0xFF;
        boolean png = imagen.length >= 8 && (imagen[0] & 0xFF) == 0x89
                && imagen[1] == 0x50 && imagen[2] == 0x4E && imagen[3] == 0x47
                && imagen[4] == 0x0D && imagen[5] == 0x0A
                && imagen[6] == 0x1A && imagen[7] == 0x0A;
        if (!jpeg && !png) {
            throw new IllegalArgumentException("El contenido del archivo no corresponde a una imagen JPEG o PNG");
        }
    }

}
