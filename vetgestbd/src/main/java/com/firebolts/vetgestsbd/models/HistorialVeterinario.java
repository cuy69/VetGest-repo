package com.firebolts.vetgestsbd.models;

import java.time.LocalDate;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
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
@Table(name = "historiales_veterinarios")
public class HistorialVeterinario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id; // Identificador único del registro de historial

    @Column(nullable = false)
    @NotNull(message = "La fecha de la atención es obligatoria")
    @PastOrPresent(message = "La fecha de la atención no puede ser futura")
    private LocalDate fecha; // Fecha en que se atendió a la mascota

    @Column(nullable = false, length = 500)
    @NotBlank(message = "El diagnóstico es obligatorio")
    @Size(max = 500, message = "El diagnóstico no puede superar los 500 caracteres")
    private String diagnostico; // Diagnóstico del veterinario

    @Column(length = 500)
    @Size(max = 500, message = "El tratamiento no puede superar los 500 caracteres")
    private String tratamiento; // Tratamiento o medicación indicada, campo opcional

    @Column(length = 1000)
    @Size(max = 1000, message = "Las observaciones no pueden superar los 1000 caracteres")
    private String observaciones; // Notas adicionales, campo opcional

    @Positive(message = "El peso debe ser mayor a 0")
    private Double peso; // Peso de la mascota en esa atención (kg), campo opcional

    @NotNull(message = "El historial debe pertenecer a una mascota")
    @ManyToOne(optional = false)
    @JoinColumn(name = "mascota_id", nullable = false)
    private Mascota mascota; // Mascota a la que pertenece el registro

    @OneToOne
    @JoinColumn(name = "cita_id", unique = true)
    private Cita cita; // Cita que originó la atención, campo opcional
}