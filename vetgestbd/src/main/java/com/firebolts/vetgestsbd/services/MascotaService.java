package com.firebolts.vetgestsbd.services;

import java.util.List;

import com.firebolts.vetgestsbd.models.Mascota;

public interface MascotaService {

    Mascota registrarMascota(Mascota mascota, long propietarioId); // HU06
    Mascota getMascotaById(Long id);                                // HU04
    List<Mascota> getMascotasPorPropietario(long propietarioId);    // HU04
    List<Mascota> getTodasMascotas();
    Mascota actualizarMascota(Mascota mascota);
    void eliminarMascota(Long id);
}