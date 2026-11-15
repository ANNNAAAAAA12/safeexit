package safeexit_backend.excepciones;

public class ConflictoException extends RuntimeException {
    public ConflictoException(String message) {
        super(message);
    }
}
