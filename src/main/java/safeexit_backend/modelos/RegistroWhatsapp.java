package safeexit_backend.modelos;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "registro_whatsapp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroWhatsapp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telefono_destino", length = 30)
    private String numeroDestino;
    
    @Column(length = 500)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_entrega", length = 30)
    private EstadoEntrega estado;

    @Column(name = "id_proveedor", length = 100)
    private String idProveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alerta_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private EventoAlerta alerta;

    @Column(name = "numero_destino", insertable = false, updatable = false, length = 30)
    private String numeroDestinoAnterior;

    @Column(name = "estado", insertable = false, updatable = false, length = 30)
    private String estadoAnterior;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @PrePersist
    public void prePersist() {
        if (this.fechaEnvio == null) {
            this.fechaEnvio = LocalDateTime.now();
        }
    }
}