package safeexit_backend.modelos;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lecturas_salida")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LecturaSalida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String placaDetectada;

    @Column(name = "fecha_hora_lectura")
    private LocalDateTime fechaHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Vehiculo vehiculo;

    private Boolean documentosAlDia;

    @Column(name = "tipo_imagen", length = 100)
    private String tipoImagen;

    @Column(name = "imagen_salida")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private byte[] imagenSalida;

    @Column(name = "fecha_hora", insertable = false, updatable = false)
    private LocalDateTime fechaHoraAnterior;

    @PrePersist
    public void prePersist() {
        if (this.fechaHora == null) {
            this.fechaHora = LocalDateTime.now();
        }
    }
}