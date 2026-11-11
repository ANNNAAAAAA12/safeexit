package safeexit_backend.config;

import safeexit_backend.modelos.*;
import safeexit_backend.repositorios.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository usuarioRepo, VehiculoRepository vehiculoRepo) {
        return args -> {
            if (usuarioRepo.count() == 0) {
                // 1. Crear Administrador de prueba
                Usuario admin = Usuario.builder()
                        .nombreCompleto("Administrador Conjunto")
                        .correo("admin@safeexit.co")
                        .contrasena("admin123")
                        .telefonoWhatsapp("+573001234567")
                        .rol("ROLE_ADMIN")
                        .build();
                usuarioRepo.save(admin);

                // 2. Crear Residente de prueba
                Usuario residente = Usuario.builder()
                        .nombreCompleto("Carlos Rodríguez")
                        .correo("carlos@gmail.com")
                        .contrasena("residente123")
                        .telefonoWhatsapp("+573109876543")
                        .rol("ROLE_RESIDENTE")
                        .build();
                usuarioRepo.save(residente);

                // 3. Crear Vehículo de prueba con SOAT vencido
                Vehiculo vehiculo = Vehiculo.builder()
                        .placa("ABC123")
                        .marca("Mazda")
                        .modelo("CX-30")
                        .fechaVencimientoSoat(LocalDate.now().minusDays(5)) // Vencido hace 5 días
                        .fechaVencimientoTecno(LocalDate.now().plusMonths(6))
                        .propietario(residente)
                        .build();
                vehiculoRepo.save(vehiculo);

                System.out.println("✅ ¡Base de datos poblada exitosamente con datos de prueba!");
            }
        };
    }
}