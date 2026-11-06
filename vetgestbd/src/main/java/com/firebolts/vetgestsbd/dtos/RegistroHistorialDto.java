package com.firebolts.vetgestsbd.dtos;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroHistorialDto {

    @NotNull(message = "El ID de la mascota es obligatorio")
    private Long mascotaId;

    // Opcional: ID de la cita asociada (puede ser null si no proviene de una cita)
    private Long citaId;

    @NotNull(message = "La fecha de atención es obligatoria")
    @PastOrPresent(message = "La fecha de la atención no puede ser futura")
    private LocalDate fecha;

    @NotBlank(message = "El diagnóstico es obligatorio")
    @Size(max = 500, message = "El diagnóstico no puede superar los 500 caracteres")
    private String diagnostico;

    @Size(max = 500, message = "El tratamiento no puede superar los 500 caracteres")
    private String tratamiento;

    @Size(max = 1000, message = "Las observaciones no pueden superar los 1000 caracteres")
    private String observaciones;

    @Positive(message = "El peso debe ser mayor a 0")
    private Double peso;
}

