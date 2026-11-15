package safeexit_backend.servicios;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import safeexit_backend.dto.CacheEstadoResponse;
import safeexit_backend.modelos.Vehiculo;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VehiculoValidacionCache {

    private static final int CAPACIDAD_MAXIMA = 1_000;
    private static final Duration TIEMPO_VIDA = Duration.ofSeconds(60);

    private final ConcurrentHashMap<String, Entrada> entradas = new ConcurrentHashMap<>();
    private final boolean habilitado;

    public VehiculoValidacionCache(@Value("${safeexit.cache.enabled:true}") boolean habilitado) {
        this.habilitado = habilitado;
    }

    public Optional<ResumenVehiculo> obtener(String placa) {
        if (!habilitado) {
            return Optional.empty();
        }
        String llave = normalizar(placa);
        Entrada entrada = entradas.get(llave);
        if (entrada == null) {
            return Optional.empty();
        }
        if (!entrada.expiraEn().isAfter(Instant.now())) {
            entradas.remove(llave, entrada);
            return Optional.empty();
        }
        return Optional.of(entrada.resumen());
    }

    public void guardar(ResumenVehiculo resumen) {
        if (!habilitado) {
            return;
        }
        if (entradas.size() >= CAPACIDAD_MAXIMA) {
            limpiarExpiradas();
            if (entradas.size() >= CAPACIDAD_MAXIMA) {
                entradas.keySet().stream().findFirst().ifPresent(entradas::remove);
            }
        }
        entradas.put(normalizar(resumen.placa()),
                new Entrada(resumen, Instant.now().plus(TIEMPO_VIDA)));
    }

    public void invalidar(String placa) {
        entradas.remove(normalizar(placa));
    }

    public CacheEstadoResponse estado() {
        limpiarExpiradas();
        return new CacheEstadoResponse(habilitado, habilitado ? entradas.size() : 0,
                habilitado ? "Memoria local (TTL 60 s, máximo 1000)" : "Deshabilitado");
    }

    public ResumenVehiculo resumir(Vehiculo vehiculo) {
        var documentos = vehiculo.getVencimientoDocumentos();
        if (documentos == null) {
            return new ResumenVehiculo(vehiculo.getId(), vehiculo.getPlaca(),
                    vehiculo.getPropietario().getTelefonoWhatsapp(),
                    vehiculo.getPropietario().getConjunto().getId(), null, null);
        }
        return new ResumenVehiculo(vehiculo.getId(), vehiculo.getPlaca(),
                vehiculo.getPropietario().getTelefonoWhatsapp(),
                vehiculo.getPropietario().getConjunto().getId(),
                documentos.getFechaVencSoat(), documentos.getFechaVencRtm());
    }

    private void limpiarExpiradas() {
        Instant ahora = Instant.now();
        entradas.entrySet().removeIf(entry -> !entry.getValue().expiraEn().isAfter(ahora));
    }

    private String normalizar(String placa) {
        return placa.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private record Entrada(ResumenVehiculo resumen, Instant expiraEn) {
    }

    public record ResumenVehiculo(
            Long vehiculoId,
            String placa,
            String telefono,
            Long conjuntoId,
            LocalDate fechaVencSoat,
            LocalDate fechaVencRtm) {
    }
}
