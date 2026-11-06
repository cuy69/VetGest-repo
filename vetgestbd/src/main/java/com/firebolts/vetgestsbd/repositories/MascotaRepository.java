package com.firebolts.vetgestsbd.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.firebolts.vetgestsbd.models.Mascota;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    List<Mascota> findByPropietarioId(long propietarioId); // Mascotas de un propietario (HU04)
}