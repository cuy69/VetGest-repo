package com.firebolts.vetgestsbd.models;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
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
@Table(name = "mascotas")
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id; // Identificador único de la mascota

    @Column(nullable = false, length = 50)
    @NotBlank(message = "El nombre de la mascota es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre; // Nombre de la mascota

    @Column(nullable = false, length = 30)
    @NotBlank(message = "La especie es obligatoria")
    private String especie; // Perro, gato, ave, etc.

    @Column(length = 50)
    private String raza; // Raza de la mascota, campo opcional

    @Column(name = "fecha_nacimiento")
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate fechaNacimiento; // Fecha de nacimiento (la edad se calcula a partir de ella)

    @Column(nullable = false, length = 10)
    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "^(?i)(MACHO|HEMBRA)$", message = "El sexo debe ser MACHO o HEMBRA")
    private String sexo; // MACHO o HEMBRA

    @Positive(message = "El peso debe ser mayor a 0")
    private Double peso; // Peso en kilogramos, campo opcional

    @Column(name = "photo", columnDefinition = "LONGTEXT")
    private String foto; // Foto de la mascota en base64, campo opcional

    @NotNull(message = "La mascota debe tener un propietario")
    @ManyToOne(optional = false)
    @JoinColumn(name = "propietario_id", nullable = false)
    private Usuario propietario; // Usuario (cliente) dueño de la mascota

    @OneToMany(mappedBy = "mascota", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Cita> citas;

    @OneToMany(mappedBy = "mascota", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<HistorialVeterinario> historiales;
}