package safeexit_backend.modelos;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parametros_sistema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametrosSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conjunto_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ConjuntoResidencial conjunto;

    @Column(name = "dias_anticipacion", nullable = false)
    @Builder.Default
    private int diasAnticipacion = 5;
}
