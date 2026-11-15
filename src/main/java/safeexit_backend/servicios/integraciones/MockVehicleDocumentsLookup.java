package safeexit_backend.servicios.integraciones;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Locale;

@Service
@ConditionalOnProperty(name = "safeexit.integrations.documents-mode", havingValue = "mock", matchIfMissing = true)
public class MockVehicleDocumentsLookup implements VehicleDocumentsLookup {

    @Override
    public DocumentDates consultar(String placa) {
        String normalizedPlate = placa.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        int seed = normalizedPlate.hashCode();
        int soatDays = Math.floorMod(seed, 365) - 15;
        int rtmDays = Math.floorMod(seed * 31, 365) - 15;
        LocalDate today = LocalDate.now();
        return new DocumentDates(today.plusDays(soatDays), today.plusDays(rtmDays));
    }
}
