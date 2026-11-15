package safeexit_backend.servicios.integraciones;

public interface PlateOcrService {
    String reconocerPlaca(byte[] imagen, String tipoContenido);
}
