package safeexit_backend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RenderDatabaseConfigurationTests {

    @Test
    void convertsRenderPostgresUrlToJdbcWithoutEmbeddingCredentials() {
        String jdbcUrl = RenderDatabaseConfiguration.toJdbcUrl(
                "postgresql://user:secret@db.example.test:5432/safeexit?sslmode=require");

        assertEquals("jdbc:postgresql://db.example.test:5432/safeexit?sslmode=require", jdbcUrl);
    }

    @Test
    void rejectsInvalidDatabaseUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> RenderDatabaseConfiguration.toJdbcUrl("https://db.example.test/safeexit"));
    }
}
