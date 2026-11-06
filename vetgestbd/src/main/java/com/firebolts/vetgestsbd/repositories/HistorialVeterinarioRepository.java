package com.firebolts.vetgestsbd.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.firebolts.vetgestsbd.models.HistorialVeterinario;

public interface HistorialVeterinarioRepository extends JpaRepository<HistorialVeterinario, Long> {

    List<HistorialVeterinario> findByMascotaIdOrderByFechaDesc(long mascotaId); // Historial de una mascota, más reciente primero (HU03)

    boolean existsByCitaId(long citaId); // Una cita solo puede generar un registro
}