package safeexit_backend.modelos;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_alerta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lectura_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private LecturaSalida lectura;

    @Column(name = "motivo_alerta", nullable = false, length = 50)
    private String motivoAlerta;

    @Column(name = "es_critico", nullable = false)
    private boolean critico;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
