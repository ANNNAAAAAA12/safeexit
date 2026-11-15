package safeexit_backend.modelos;

public enum RolUsuario {
    ADMIN,
    RESIDENT,
    AUDITOR;

    public String autoridad() {
        return "ROLE_" + name();
    }
}
