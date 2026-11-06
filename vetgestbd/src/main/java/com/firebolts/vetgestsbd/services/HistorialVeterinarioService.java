package com.firebolts.vetgestsbd.services;

import java.util.List;

import com.firebolts.vetgestsbd.models.HistorialVeterinario;

public interface HistorialVeterinarioService {

    // citaId es opcional (puede ser null si fue una consulta sin cita previa)
    HistorialVeterinario registrarHistorial(HistorialVeterinario historial, long mascotaId, Long citaId);
    HistorialVeterinario getHistorialById(Long id);
    List<HistorialVeterinario> getHistorialPorMascota(long mascotaId); // HU03
    HistorialVeterinario actualizarHistorial(HistorialVeterinario historial);
}