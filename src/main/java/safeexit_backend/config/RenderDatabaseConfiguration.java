package safeexit_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "database.url")
public class RenderDatabaseConfiguration {

    @Bean
    @Primary
    DataSource renderDataSource(
            @Value("${DATABASE_URL}") String databaseUrl,
            @Value("${DB_USER}") String username,
            @Value("${DB_PASSWORD}") String password) {
        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(toJdbcUrl(databaseUrl))
                .username(username)
                .password(password)
                .build();
    }

    static String toJdbcUrl(String databaseUrl) {
        URI uri = URI.create(databaseUrl);
        if (!("postgresql".equals(uri.getScheme()) || "postgres".equals(uri.getScheme()))
                || uri.getHost() == null
                || uri.getRawPath() == null
                || uri.getRawPath().length() < 2) {
            throw new IllegalArgumentException("DATABASE_URL debe ser una URL válida de PostgreSQL");
        }

        int port = uri.getPort() < 0 ? 5432 : uri.getPort();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        return "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getRawPath() + query;
    }
}
