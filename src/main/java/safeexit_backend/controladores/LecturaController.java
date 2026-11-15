package safeexit_backend.controladores;

import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import safeexit_backend.dto.ProcesarSalidaResponse;
import safeexit_backend.servicios.ProcesamientoSalidaService;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/lecturas")
public class LecturaController {

    private final ProcesamientoSalidaService procesamientoService;

    public LecturaController(ProcesamientoSalidaService procesamientoService) {
        this.procesamientoService = procesamientoService;
    }

    @PostMapping(value = "/procesar-salida", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProcesarSalidaResponse procesarSalida(@RequestPart("imagen") @NotNull MultipartFile imagen)
            throws IOException {
        return procesamientoService.procesar(imagen.getBytes(), imagen.getContentType());
    }
}
