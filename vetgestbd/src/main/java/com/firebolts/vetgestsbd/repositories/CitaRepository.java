package com.firebolts.vetgestsbd.repositories;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.firebolts.vetgestsbd.models.Cita;
import com.firebolts.vetgestsbd.models.EstadoCita;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByMascotaPropietarioId(long propietarioId); // Citas de un propietario (HU12)

    List<Cita> findByMascotaId(long mascotaId); // Citas de una mascota

    List<Cita> findByEstado(EstadoCita estado); // Filtro por estado (ADMIN)

    // Evita dos citas a la misma fecha y hora (se usa con EstadoCita.CANCELADA) al CREAR
    boolean existsByFechaAndHoraAndEstadoNot(LocalDate fecha, LocalTime hora, EstadoCita estado);

    // Igual que la anterior pero excluye la propia cita, para usarla al ACTUALIZAR
    boolean existsByFechaAndHoraAndEstadoNotAndIdNot(LocalDate fecha, LocalTime hora, EstadoCita estado, long id);
}