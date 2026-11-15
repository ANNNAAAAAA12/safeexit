package safeexit_backend.modelos;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "vencimiento_documentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VencimientoDocumentos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Vehiculo vehiculo;

    @Column(name = "fecha_venc_soat", nullable = false)
    private LocalDate fechaVencSoat;

    @Column(name = "fecha_venc_rtm", nullable = false)
    private LocalDate fechaVencRtm;
}
