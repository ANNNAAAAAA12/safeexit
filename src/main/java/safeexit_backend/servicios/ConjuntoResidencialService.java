package safeexit_backend.servicios;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import safeexit_backend.dto.ActualizarConjuntoRequest;
import safeexit_backend.dto.ConjuntoResponse;
import safeexit_backend.excepciones.RecursoNoEncontradoException;
import safeexit_backend.modelos.ConjuntoResidencial;
import safeexit_backend.repositorios.ConjuntoResidencialRepository;

import java.util.List;

@Service
public class ConjuntoResidencialService {

    private final ConjuntoResidencialRepository conjuntoRepository;

    public ConjuntoResidencialService(ConjuntoResidencialRepository conjuntoRepository) {
        this.conjuntoRepository = conjuntoRepository;
    }

    @Transactional(readOnly = true)
    public List<ConjuntoResponse> listar() {
        return conjuntoRepository.findAll().stream().map(this::mapear).toList();
    }

    @Transactional
    public ConjuntoResponse actualizar(Long id, ActualizarConjuntoRequest request) {
        ConjuntoResidencial conjunto = conjuntoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Conjunto residencial no encontrado"));
        conjunto.setNombre(request.nombre().trim());
        conjunto.setDireccion(request.direccion().trim());
        conjunto.setNit(request.nit().trim());
        return mapear(conjuntoRepository.save(conjunto));
    }

    private ConjuntoResponse mapear(ConjuntoResidencial conjunto) {
        return new ConjuntoResponse(conjunto.getId(), conjunto.getNombre(),
                conjunto.getDireccion(), conjunto.getNit());
    }
}
