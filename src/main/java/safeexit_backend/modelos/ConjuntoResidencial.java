package safeexit_backend.modelos;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "conjuntos_residenciales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConjuntoResidencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String direccion;

    @Column(length = 50)
    private String ciudad;
}