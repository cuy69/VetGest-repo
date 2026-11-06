package com.firebolts.vetgestsbd.models;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "servicios_veterinarios")
public class ServicioVeterinario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id; // Identificador único del servicio

    @Column(nullable = false, unique = true, length = 100)
    @NotBlank(message = "El nombre del servicio es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre; // Ej: Consulta general, Vacunación, Baño y corte

    @Column(length = 500)
    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    private String descripcion; // Detalle de lo que incluye el servicio, campo opcional

    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    private BigDecimal precio; // Precio del servicio (BigDecimal para evitar errores de redondeo)

    @Column(name = "duracion_minutos")
    @Positive(message = "La duración debe ser mayor a 0")
    private Integer duracionMinutos; // Duración estimada en minutos, campo opcional

    @Column(nullable = false)
    private boolean activo = true; // Permite ocultar un servicio sin borrarlo (HU13 muestra solo los activos)

    @OneToMany(mappedBy = "servicio")
    @JsonIgnore
    private List<Cita> citas; // Citas que usan este servicio
}