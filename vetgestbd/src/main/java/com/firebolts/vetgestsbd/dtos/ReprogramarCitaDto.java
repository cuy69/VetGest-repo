package com.firebolts.vetgestsbd.dtos;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReprogramarCitaDto {

    @NotNull(message = "La nueva fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La nueva hora es obligatoria")
    private LocalTime hora;
}

