package safeexit_backend.modelos;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lecturas_salida")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LecturaSalida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String placaDetectada;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;

    private Boolean documentosAlDia;

    @PrePersist
    public void prePersist() {
        this.fechaHora = LocalDateTime.now();
    }
}