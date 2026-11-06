package com.firebolts.vetgestsbd.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.firebolts.vetgestsbd.exceptions.RecursoNoEncontradoException;
import com.firebolts.vetgestsbd.models.Cita;
import com.firebolts.vetgestsbd.models.EstadoCita;
import com.firebolts.vetgestsbd.models.Mascota;
import com.firebolts.vetgestsbd.models.ServicioVeterinario;
import com.firebolts.vetgestsbd.repositories.CitaRepository;
import com.firebolts.vetgestsbd.repositories.MascotaRepository;
import com.firebolts.vetgestsbd.repositories.ServicioVeterinarioRepository;


@Service(value = "cService")
public class CitaServiceImpl implements CitaService {

    private final CitaRepository cRepository;
    private final MascotaRepository mRepository;
    private final ServicioVeterinarioRepository svRepository;

    public CitaServiceImpl(CitaRepository cRepository,
                           MascotaRepository mRepository,
                           ServicioVeterinarioRepository svRepository) {
        this.cRepository = cRepository;
        this.mRepository = mRepository;
        this.svRepository = svRepository;
    }

    // Solicitar una cita (HU12): siempre nace PENDIENTE
    @Override
    @Transactional
    public Cita solicitarCita(Cita cita, long mascotaId, long servicioId) {
        if (cita == null) {
            throw new IllegalArgumentException("La cita no puede ser nula");
        }
        validarFechaHoraFutura(cita.getFecha(), cita.getHora());

        Mascota mascota = mRepository.findById(mascotaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada con id: " + mascotaId));
        ServicioVeterinario servicio = svRepository.findById(servicioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado con id: " + servicioId));

        if (!servicio.isActivo()) {
            throw new IllegalStateException("El servicio seleccionado no está disponible");
        }
        if (cRepository.existsByFechaAndHoraAndEstadoNot(cita.getFecha(), cita.getHora(), EstadoCita.CANCELADA)) {
            throw new IllegalStateException("Ya existe una cita en esa fecha y hora");
        }

        cita.setId(0);
        cita.setMascota(mascota);
        cita.setServicio(servicio);
        cita.setEstado(EstadoCita.PENDIENTE);
        return cRepository.save(cita);
    }

    @Override
    public Cita getCitaById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        return cRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada con id: " + id));
    }

    @Override
    public List<Cita> getTodasCitas() {
        return cRepository.findAll();
    }

    @Override
    public List<Cita> getCitasPorEstado(EstadoCita estado) {
        return cRepository.findByEstado(estado);
    }

    @Override
    public List<Cita> getCitasPorPropietario(long propietarioId) {
        return cRepository.findByMascotaPropietarioId(propietarioId);
    }

    @Override
    public List<Cita> getCitasPorMascota(long mascotaId) {
        return cRepository.findByMascotaId(mascotaId);
    }

    // Reprogramar: solo citas PENDIENTE o CONFIRMADA
    @Override
    @Transactional
    public Cita reprogramarCita(Long id, LocalDate fecha, LocalTime hora) {
        Cita cita = getCitaById(id);

        if (cita.getEstado() == EstadoCita.CANCELADA || cita.getEstado() == EstadoCita.COMPLETADA) {
            throw new IllegalStateException("No se puede reprogramar una cita " + cita.getEstado());
        }
        validarFechaHoraFutura(fecha, hora);

        if (cRepository.existsByFechaAndHoraAndEstadoNotAndIdNot(fecha, hora, EstadoCita.CANCELADA, cita.getId())) {
            throw new IllegalStateException("Ya existe una cita en esa fecha y hora");
        }

        cita.setFecha(fecha);
        cita.setHora(hora);
        cita.setEstado(EstadoCita.PENDIENTE); // vuelve a requerir confirmación
        return cRepository.save(cita);
    }

    // Cambiar estado respetando las transiciones válidas
    @Override
    @Transactional
    public Cita cambiarEstado(Long id, EstadoCita nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El estado no puede ser nulo");
        }
        Cita cita = getCitaById(id);
        EstadoCita actual = cita.getEstado();

        boolean valida = switch (actual) {
            case PENDIENTE -> nuevoEstado == EstadoCita.CONFIRMADA || nuevoEstado == EstadoCita.CANCELADA;
            case CONFIRMADA -> nuevoEstado == EstadoCita.COMPLETADA || nuevoEstado == EstadoCita.CANCELADA;
            case COMPLETADA, CANCELADA -> false;
        };

        if (!valida) {
            throw new IllegalStateException("No se puede pasar de " + actual + " a " + nuevoEstado);
        }

        cita.setEstado(nuevoEstado);
        return cRepository.save(cita);
    }

    private void validarFechaHoraFutura(LocalDate fecha, LocalTime hora) {
        if (fecha == null || hora == null) {
            throw new IllegalArgumentException("La fecha y la hora son obligatorias");
        }
        if (LocalDateTime.of(fecha, hora).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha y hora de la cita no pueden estar en el pasado");
        }
    }
}