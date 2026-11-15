package safeexit_backend.modelos;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehiculos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String placa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo", length = 20)
    private TipoVehiculo tipoVehiculo;

    @Column(length = 50)
    private String marca;

    @Column(length = 50)
    private String modelo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario propietario;

    @OneToOne(mappedBy = "vehiculo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VencimientoDocumentos vencimientoDocumentos;

    @Column(name = "usuario_id", insertable = false, updatable = false)
    private Long propietarioIdAnterior;

    @Column(name = "fecha_vencimiento_soat", insertable = false, updatable = false)
    private java.time.LocalDate fechaVencimientoSoatAnterior;

    @Column(name = "fecha_vencimiento_tecno", insertable = false, updatable = false)
    private java.time.LocalDate fechaVencimientoRtmAnterior;
}