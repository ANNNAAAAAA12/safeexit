package safeexit_backend.servicios;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MockDocumentVerificationServiceTests {

    private final MockDocumentVerificationService service = new MockDocumentVerificationService();

    @Test
    void warnsWhenDocumentExpiresWithinConfiguredWindow() {
        LocalDate today = LocalDate.of(2026, 11, 14);

        var result = service.verificar(today.plusDays(5), today.plusDays(30), today, 5);

        assertTrue(result.documentosAlDia());
        assertFalse(result.critica());
        assertEquals("POR_VENCER", result.soat().estado());
        assertEquals(java.util.List.of("SOAT_POR_VENCER"), result.motivosAlerta());
    }

    @Test
    void marksExpiredDocumentsAsCritical() {
        LocalDate today = LocalDate.of(2026, 11, 14);

        var result = service.verificar(today.minusDays(1), today, today, 5);

        assertFalse(result.documentosAlDia());
        assertTrue(result.critica());
        assertEquals(java.util.List.of("SOAT_VENCIDO", "RTM_POR_VENCER"), result.motivosAlerta());
    }
}
