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
        int scenario = Math.floorMod(seed, 3);
        LocalDate today = LocalDate.now();
        LocalDate soat = switch (scenario) {
            case 0 -> today.minusDays(1);
            case 1 -> today.plusDays(3);
            default -> today.plusDays(120);
        };
        LocalDate rtm = switch (scenario) {
            case 0 -> today.plusDays(30);
            case 1 -> today.plusDays(90);
            default -> today.plusDays(180);
        };
        return new DocumentDates(soat, rtm);
    }
}
