package safeexit_backend.controladores;

import safeexit_backend.modelos.Vehiculo;
import safeexit_backend.repositorios.VehiculoRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehiculos")
public class VehiculoController {

    private final VehiculoRepository vehiculoRepository;

    public VehiculoController(VehiculoRepository vehiculoRepository) {
        this.vehiculoRepository = vehiculoRepository;
    }

    @GetMapping
    public List<Vehiculo> obtenerTodos() {
        return vehiculoRepository.findAll();
    }
}