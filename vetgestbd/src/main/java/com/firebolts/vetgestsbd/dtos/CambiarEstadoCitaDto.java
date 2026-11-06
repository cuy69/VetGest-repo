package com.firebolts.vetgestsbd.dtos;

import com.firebolts.vetgestsbd.models.EstadoCita;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CambiarEstadoCitaDto {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoCita estado;
}

