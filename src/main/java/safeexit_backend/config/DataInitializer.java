package safeexit_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import safeexit_backend.modelos.ConjuntoResidencial;
import safeexit_backend.modelos.EstadoEntrega;
import safeexit_backend.modelos.LecturaSalida;
import safeexit_backend.modelos.ParametrosSistema;
import safeexit_backend.modelos.RegistroWhatsapp;
import safeexit_backend.modelos.RolUsuario;
import safeexit_backend.modelos.Usuario;
import safeexit_backend.modelos.Vehiculo;
import safeexit_backend.modelos.VencimientoDocumentos;
import safeexit_backend.repositorios.ConjuntoResidencialRepository;
import safeexit_backend.repositorios.LecturaSalidaRepository;
import safeexit_backend.repositorios.ParametrosSistemaRepository;
import safeexit_backend.repositorios.RegistroWhatsappRepository;
import safeexit_backend.repositorios.UsuarioRepository;
import safeexit_backend.repositorios.VehiculoRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner inicializarDatos(
            ConjuntoResidencialRepository conjuntos,
            ParametrosSistemaRepository parametros,
            UsuarioRepository usuarios,
            VehiculoRepository vehiculos,
            LecturaSalidaRepository lecturas,
            RegistroWhatsappRepository notificaciones,
            PasswordEncoder passwordEncoder,
            @Value("${safeexit.residential.name:SafeExit}") String nombreConjunto,
            @Value("${safeexit.residential.address:Dirección por configurar}") String direccionConjunto,
            @Value("${safeexit.residential.nit:DEMO-0001}") String nitConjunto,
            @Value("${safeexit.bootstrap.admin-email:}") String correoAdministrador,
            @Value("${safeexit.bootstrap.admin-password:}") String contrasenaAdministrador,
            @Value("${safeexit.bootstrap.admin-phone:}") String telefonoAdministrador) {
        return args -> {
            vehiculos.migrarPropietariosAnteriores();
            List<ConjuntoResidencial> existentes = conjuntos.findAll();
            if (existentes.isEmpty()) {
                existentes = List.of(conjuntos.save(ConjuntoResidencial.builder()
                            .nombre(nombreConjunto)
                            .direccion(direccionConjunto)
                            .nit(nitConjunto)
                            .build()));
            }
            for (int index = 0; index < existentes.size(); index++) {
                ConjuntoResidencial conjunto = existentes.get(index);
                if (conjunto.getNit() == null || conjunto.getNit().isBlank()) {
                    conjunto.setNit(index == 0 ? nitConjunto : nitConjunto + "-" + conjunto.getId());
                    conjuntos.save(conjunto);
                }
                if (parametros.findByConjuntoId(conjunto.getId()).isEmpty()) {
                    parametros.save(ParametrosSistema.builder().conjunto(conjunto).diasAnticipacion(5).build());
                }
            }

            ConjuntoResidencial conjuntoPredeterminado = existentes.get(0);
            for (Usuario usuario : usuarios.findAll()) {
                boolean cambiado = false;
                if (usuario.getConjunto() == null) {
                    usuario.setConjunto(conjuntoPredeterminado);
                    cambiado = true;
                }
                if (usuario.getActivo() == null) {
                    usuario.setActivo(true);
                    cambiado = true;
                }
                if ("ROLE_RESIDENTE".equalsIgnoreCase(usuario.getRol())
                        || "RESIDENTE".equalsIgnoreCase(usuario.getRol())) {
                    usuario.setRol(RolUsuario.RESIDENT.autoridad());
                    cambiado = true;
                } else if (!usuario.getRol().startsWith("ROLE_")) {
                    usuario.setRol("ROLE_" + usuario.getRol().toUpperCase(Locale.ROOT));
                    cambiado = true;
                }
                String hash = usuario.getContrasena();
                boolean passwordSinHash = hash != null && !hash.startsWith("$2a$")
                        && !hash.startsWith("$2b$") && !hash.startsWith("$2y$");
                if (passwordSinHash) {
                    usuario.setContrasena(passwordEncoder.encode(hash));
                    cambiado = true;
                }
                if (("admin@safeexit.co".equalsIgnoreCase(usuario.getCorreo())
                        && passwordSinHash)
                        || ("carlos@gmail.com".equalsIgnoreCase(usuario.getCorreo())
                        && passwordSinHash)) {
                    usuario.setActivo(false);
                    cambiado = true;
                }
                if (cambiado) {
                    usuarios.save(usuario);
                }
            }

            for (Vehiculo vehiculo : vehiculos.findAll()) {
                boolean cambiado = false;
                if (vehiculo.getTipoVehiculo() == null) {
                    vehiculo.setTipoVehiculo(safeexit_backend.modelos.TipoVehiculo.CARRO);
                    cambiado = true;
                }
                if (vehiculo.getVencimientoDocumentos() == null
                        && vehiculo.getFechaVencimientoSoatAnterior() != null
                        && vehiculo.getFechaVencimientoRtmAnterior() != null) {
                    VencimientoDocumentos documentos = VencimientoDocumentos.builder()
                            .vehiculo(vehiculo)
                            .fechaVencSoat(vehiculo.getFechaVencimientoSoatAnterior())
                            .fechaVencRtm(vehiculo.getFechaVencimientoRtmAnterior())
                            .build();
                    vehiculo.setVencimientoDocumentos(documentos);
                    cambiado = true;
                }
                if (cambiado) {
                    vehiculos.save(vehiculo);
                }
            }

            for (LecturaSalida lectura : lecturas.findAll()) {
                if (lectura.getFechaHora() == null) {
                    lectura.setFechaHora(lectura.getFechaHoraAnterior() == null
                            ? java.time.LocalDateTime.now()
                            : lectura.getFechaHoraAnterior());
                    lecturas.save(lectura);
                }
            }

            for (RegistroWhatsapp notificacion : notificaciones.findAll()) {
                boolean cambiado = false;
                if (notificacion.getNumeroDestino() == null) {
                    notificacion.setNumeroDestino(notificacion.getNumeroDestinoAnterior());
                    cambiado = notificacion.getNumeroDestino() != null;
                }
                if (notificacion.getEstado() == null) {
                    notificacion.setEstado(convertirEstadoEntrega(notificacion.getEstadoAnterior()));
                    cambiado = true;
                }
                if (notificacion.getFechaEnvio() == null) {
                    notificacion.setFechaEnvio(java.time.LocalDateTime.now());
                    cambiado = true;
                }
                if (cambiado) {
                    notificaciones.save(notificacion);
                }
            }

            boolean emailConfigurado = !correoAdministrador.isBlank();
            boolean passwordConfigurado = !contrasenaAdministrador.isBlank();
            if (emailConfigurado != passwordConfigurado) {
                throw new IllegalStateException(
                        "Configure SAFEEXIT_ADMIN_EMAIL y SAFEEXIT_ADMIN_PASSWORD juntos");
            }
            if (!emailConfigurado) {
                return;
            }
            if (telefonoAdministrador.isBlank()) {
                throw new IllegalStateException(
                        "Configure SAFEEXIT_ADMIN_PHONE para crear la cuenta de administrador");
            }
            if (!correoAdministrador.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new IllegalStateException("SAFEEXIT_ADMIN_EMAIL debe ser un correo válido");
            }
            if (!telefonoAdministrador.matches("^\\+57[0-9]{10}$")) {
                throw new IllegalStateException("SAFEEXIT_ADMIN_PHONE debe usar el formato +57 seguido de 10 dígitos");
            }
            int longitudContrasena = contrasenaAdministrador.getBytes(StandardCharsets.UTF_8).length;
            if (longitudContrasena < 12 || longitudContrasena > 72) {
                throw new IllegalStateException("SAFEEXIT_ADMIN_PASSWORD debe tener entre 12 y 72 bytes UTF-8");
            }

            String correo = correoAdministrador.toLowerCase(Locale.ROOT);
            if (usuarios.findByCorreo(correo).isEmpty()) {
                usuarios.save(Usuario.builder()
                        .nombreCompleto("Administrador")
                        .correo(correo)
                        .contrasena(passwordEncoder.encode(contrasenaAdministrador))
                        .telefonoWhatsapp(telefonoAdministrador)
                        .rol("ROLE_ADMIN")
                        .activo(true)
                        .conjunto(conjuntoPredeterminado)
                        .build());
            }
        };
    }

    private static EstadoEntrega convertirEstadoEntrega(String estadoAnterior) {
        if (estadoAnterior == null || estadoAnterior.isBlank()) {
            return EstadoEntrega.PENDIENTE;
        }
        return switch (estadoAnterior.toUpperCase(Locale.ROOT)) {
            case "ENTREGADO", "DELIVERED", "READ" -> EstadoEntrega.ENTREGADO;
            case "FALLIDO", "FAILED", "UNDELIVERED" -> EstadoEntrega.FALLIDO;
            case "ENVIADO", "SENT" -> EstadoEntrega.ENVIADO;
            default -> EstadoEntrega.PENDIENTE;
        };
    }
}
