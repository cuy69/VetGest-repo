package com.firebolts.vetgestsbd.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.firebolts.vetgestsbd.exceptions.RecursoNoEncontradoException;
import com.firebolts.vetgestsbd.models.Cita;
import com.firebolts.vetgestsbd.models.EstadoCita;
import com.firebolts.vetgestsbd.models.HistorialVeterinario;
import com.firebolts.vetgestsbd.models.Mascota;
import com.firebolts.vetgestsbd.repositories.CitaRepository;
import com.firebolts.vetgestsbd.repositories.HistorialVeterinarioRepository;
import com.firebolts.vetgestsbd.repositories.MascotaRepository;

@Service(value = "hService")
public class HistorialVeterinarioServiceImpl implements HistorialVeterinarioService {

    private final HistorialVeterinarioRepository hRepository;
    private final MascotaRepository mRepository;
    private final CitaRepository cRepository;

    public HistorialVeterinarioServiceImpl(HistorialVeterinarioRepository hRepository,
                                           MascotaRepository mRepository,
                                           CitaRepository cRepository) {
        this.hRepository = hRepository;
        this.mRepository = mRepository;
        this.cRepository = cRepository;
    }

    // Registrar una atención en el historial de una mascota
    @Override
    @Transactional
    public HistorialVeterinario registrarHistorial(HistorialVeterinario historial, long mascotaId, Long citaId) {
        if (historial == null) {
            throw new IllegalArgumentException("El historial no puede ser nulo");
        }

        Mascota mascota = mRepository.findById(mascotaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada con id: " + mascotaId));
        historial.setMascota(mascota);

        if (citaId != null) {
            Cita cita = cRepository.findById(citaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada con id: " + citaId));

            if (cita.getMascota().getId() != mascotaId) {
                throw new IllegalArgumentException("La cita no pertenece a esa mascota");
            }
            // Solo se atiende una cita confirmada (excluye pendientes, canceladas y ya completadas)
            if (cita.getEstado() != EstadoCita.CONFIRMADA) {
                throw new IllegalStateException("Solo se puede registrar historial de una cita CONFIRMADA");
            }
            if (hRepository.existsByCitaId(citaId)) {
                throw new IllegalStateException("La cita ya tiene un historial registrado");
            }

            historial.setCita(cita);
            // Al registrar el historial, la cita pasa a COMPLETADA (HU15)
            cita.setEstado(EstadoCita.COMPLETADA);
            cRepository.save(cita);
        }

        return hRepository.save(historial);
    }

    // Obtener un registro del historial por su ID
    @Override
    public HistorialVeterinario getHistorialById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        return hRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Historial no encontrado con id: " + id));
    }

    // Historial completo de una mascota (ordenado cronológicamente)
    @Override
    public List<HistorialVeterinario> getHistorialPorMascota(long mascotaId) {
        if (!mRepository.existsById(mascotaId)) {
            throw new RecursoNoEncontradoException("Mascota no encontrada con id: " + mascotaId);
        }
        return hRepository.findByMascotaIdOrderByFechaDesc(mascotaId);
    }

    // Actualización parcial del historial
    @Override
    public HistorialVeterinario actualizarHistorial(HistorialVeterinario historial) {
        if (historial == null || historial.getId() == 0) {
            throw new IllegalArgumentException("El historial debe tener un ID válido");
        }

        HistorialVeterinario existente = hRepository.findById(historial.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Historial no encontrado con id: " + historial.getId()));

        if (historial.getFecha() != null) {
            existente.setFecha(historial.getFecha());
        }
        if (historial.getDiagnostico() != null && !historial.getDiagnostico().isBlank()) {
            existente.setDiagnostico(historial.getDiagnostico());
        }
        if (historial.getTratamiento() != null && !historial.getTratamiento().isBlank()) {
            existente.setTratamiento(historial.getTratamiento());
        }
        if (historial.getObservaciones() != null && !historial.getObservaciones().isBlank()) {
            existente.setObservaciones(historial.getObservaciones());
        }
        if (historial.getPeso() != null) {
            existente.setPeso(historial.getPeso());
        }

        return hRepository.save(existente);
    }
}
