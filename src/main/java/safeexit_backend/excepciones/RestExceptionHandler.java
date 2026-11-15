package safeexit_backend.excepciones;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> noEncontrado(
            RecursoNoEncontradoException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<RespuestaError> conflicto(
            ConflictoException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    public ResponseEntity<RespuestaError> credencialesInvalidas(
            RuntimeException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.UNAUTHORIZED, "Credenciales inválidas o cuenta deshabilitada", request);
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ResponseEntity<RespuestaError> servicioExterno(
            ServicioExternoException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespuestaError> accesoDenegado(
            AccessDeniedException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.FORBIDDEN, "No tiene permiso para esta operación", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> validacion(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String mensaje = exception.getBindingResult().getFieldErrors().stream()
                .map(this::mensajeCampo)
                .collect(Collectors.joining("; "));
        return respuesta(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class})
    public ResponseEntity<RespuestaError> solicitudInvalida(
            RuntimeException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> cuerpoInvalido(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no tiene el formato esperado", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<RespuestaError> archivoDemasiadoGrande(
            MaxUploadSizeExceededException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen supera el tamaño máximo permitido", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespuestaError> integridad(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, "La operación viola una restricción de datos", request);
    }

    private String mensajeCampo(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<RespuestaError> respuesta(
            HttpStatus status, String message, HttpServletRequest request) {
        RespuestaError body = new RespuestaError(Instant.now(), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
