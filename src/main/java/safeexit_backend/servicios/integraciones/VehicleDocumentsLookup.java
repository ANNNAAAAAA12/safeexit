package safeexit_backend.servicios.integraciones;

import java.time.LocalDate;

public interface VehicleDocumentsLookup {

    DocumentDates consultar(String placa);

    record DocumentDates(LocalDate fechaVencSoat, LocalDate fechaVencRtm) {
    }
}
