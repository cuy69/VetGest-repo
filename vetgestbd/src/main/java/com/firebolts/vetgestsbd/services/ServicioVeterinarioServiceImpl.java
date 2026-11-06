package com.firebolts.vetgestsbd.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.firebolts.vetgestsbd.exceptions.RecursoNoEncontradoException;
import com.firebolts.vetgestsbd.models.ServicioVeterinario;
import com.firebolts.vetgestsbd.repositories.ServicioVeterinarioRepository;

@Service(value = "svService")
public class ServicioVeterinarioServiceImpl implements ServicioVeterinarioService {

    private final ServicioVeterinarioRepository svRepository;

    public ServicioVeterinarioServiceImpl(ServicioVeterinarioRepository svRepository) {
        this.svRepository = svRepository;
    }

    // Crear un nuevo servicio
    @Override
    public ServicioVeterinario crearServicio(ServicioVeterinario servicio) {
        if (servicio == null) {
            throw new IllegalArgumentException("El servicio no puede ser nulo");
        }
        if (servicio.getNombre() != null && svRepository.existsByNombreIgnoreCase(servicio.getNombre())) {
            throw new IllegalStateException("Ya existe un servicio con ese nombre");
        }
        return svRepository.save(servicio);
    }

    // Obtener un servicio por su ID
    @Override
    public ServicioVeterinario getServicioById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        return svRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado con id: " + id));
    }

    // Servicios visibles para los clientes (HU13)
    @Override
    public List<ServicioVeterinario> getServiciosActivos() {
        return svRepository.findByActivoTrue();
    }

    // Todos los servicios, incluidos los desactivados (para el administrador)
    @Override
    public List<ServicioVeterinario> getTodosServicios() {
        return svRepository.findAll();
    }

    // Actualización parcial: solo sobrescribe los valores enviados
    @Override
    public ServicioVeterinario actualizarServicio(ServicioVeterinario servicio) {
        if (servicio == null || servicio.getId() == 0) {
            throw new IllegalArgumentException("El servicio debe tener un ID válido");
        }

        ServicioVeterinario existente = svRepository.findById(servicio.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado con id: " + servicio.getId()));

        if (servicio.getNombre() != null && !servicio.getNombre().isBlank()) {
            if (svRepository.existsByNombreIgnoreCaseAndIdNot(servicio.getNombre(), existente.getId())) {
                throw new IllegalStateException("Ya existe un servicio con ese nombre");
            }
            existente.setNombre(servicio.getNombre());
        }
        if (servicio.getDescripcion() != null && !servicio.getDescripcion().isBlank()) {
            existente.setDescripcion(servicio.getDescripcion());
        }
        if (servicio.getPrecio() != null) {
            existente.setPrecio(servicio.getPrecio());
        }
        if (servicio.getDuracionMinutos() != null) {
            existente.setDuracionMinutos(servicio.getDuracionMinutos());
        }

        return svRepository.save(existente);
    }

    // Activar o desactivar un servicio (no se elimina para conservar el historial de citas)
    @Override
    public ServicioVeterinario cambiarEstado(Long id, boolean activo) {
        ServicioVeterinario servicio = getServicioById(id);
        servicio.setActivo(activo);
        return svRepository.save(servicio);
    }
}
