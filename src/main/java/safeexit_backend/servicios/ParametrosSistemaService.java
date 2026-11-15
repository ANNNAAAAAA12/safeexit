package safeexit_backend.servicios;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import safeexit_backend.dto.ActualizarParametrosRequest;
import safeexit_backend.dto.ParametrosSistemaResponse;
import safeexit_backend.excepciones.RecursoNoEncontradoException;
import safeexit_backend.modelos.ParametrosSistema;
import safeexit_backend.repositorios.ConjuntoResidencialRepository;
import safeexit_backend.repositorios.ParametrosSistemaRepository;

@Service
public class ParametrosSistemaService {

    private final ParametrosSistemaRepository parametrosRepository;
    private final ConjuntoResidencialRepository conjuntoRepository;

    public ParametrosSistemaService(
            ParametrosSistemaRepository parametrosRepository,
            ConjuntoResidencialRepository conjuntoRepository) {
        this.parametrosRepository = parametrosRepository;
        this.conjuntoRepository = conjuntoRepository;
    }

    @Transactional(readOnly = true)
    public ParametrosSistemaResponse obtener(Long conjuntoId) {
        return parametrosRepository.findByConjuntoId(conjuntoId)
                .map(parametros -> new ParametrosSistemaResponse(
                        parametros.getId(), conjuntoId, parametros.getDiasAnticipacion()))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Parámetros del conjunto residencial no encontrados"));
    }

    @Transactional
    public ParametrosSistemaResponse actualizar(Long conjuntoId, ActualizarParametrosRequest request) {
        var conjunto = conjuntoRepository.findById(conjuntoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Conjunto residencial no encontrado"));
        ParametrosSistema parametros = parametrosRepository.findByConjuntoId(conjuntoId)
                .orElseGet(() -> ParametrosSistema.builder().conjunto(conjunto).build());
        parametros.setDiasAnticipacion(request.diasAnticipacion());
        ParametrosSistema guardado = parametrosRepository.save(parametros);
        return new ParametrosSistemaResponse(guardado.getId(), conjuntoId, guardado.getDiasAnticipacion());
    }
}
