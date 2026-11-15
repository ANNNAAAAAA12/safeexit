package safeexit_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import safeexit_backend.modelos.TipoVehiculo;

import java.time.LocalDate;

public record CrearVehiculoRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}[0-9]{3}$|^[A-Za-z]{3}[0-9]{2}[A-Za-z]$",
                message = "La placa debe tener el formato colombiano AAA123 o ABC12D")
        String placa,
        @NotNull TipoVehiculo tipoVehiculo,
        @Size(max = 50) String marca,
        @Size(max = 50) String modelo,
        LocalDate fechaVencSoat,
        LocalDate fechaVencRtm) {

    @AssertTrue(message = "Para el ingreso manual debe proporcionar las fechas de SOAT y RTM")
    public boolean isManualFallbackComplete() {
        return (fechaVencSoat == null) == (fechaVencRtm == null);
    }
}
