package com.firebolts.vetgestsbd.services;
 
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
 
import com.firebolts.vetgestsbd.models.Cita;
import com.firebolts.vetgestsbd.models.EstadoCita;
 
public interface CitaService {
 
    Cita solicitarCita(Cita cita, long mascotaId, long servicioId);   // HU12
    Cita getCitaById(Long id);
    List<Cita> getTodasCitas();                                       // ADMIN
    List<Cita> getCitasPorEstado(EstadoCita estado);                  // ADMIN
    List<Cita> getCitasPorPropietario(long propietarioId);            // CLIENTE
    List<Cita> getCitasPorMascota(long mascotaId);
    Cita reprogramarCita(Long id, LocalDate fecha, LocalTime hora);
    Cita cambiarEstado(Long id, EstadoCita nuevoEstado);              // confirmar, cancelar, completar
}
 