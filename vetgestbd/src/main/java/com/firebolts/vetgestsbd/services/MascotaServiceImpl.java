package com.firebolts.vetgestsbd.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.firebolts.vetgestsbd.exceptions.RecursoNoEncontradoException;
import com.firebolts.vetgestsbd.models.Mascota;
import com.firebolts.vetgestsbd.models.Usuario;
import com.firebolts.vetgestsbd.repositories.MascotaRepository;
import com.firebolts.vetgestsbd.repositories.UsuarioRepository;

@Service(value = "mService")
public class MascotaServiceImpl implements MascotaService {

    private final MascotaRepository mRepository;
    private final UsuarioRepository uRepository;

    public MascotaServiceImpl(MascotaRepository mRepository, UsuarioRepository uRepository) {
        this.mRepository = mRepository;
        this.uRepository = uRepository;
    }

    // Registrar una mascota asociada a un propietario (HU06)
    @Override
    public Mascota registrarMascota(Mascota mascota, long propietarioId) {
        if (mascota == null) {
            throw new IllegalArgumentException("La mascota no puede ser nula");
        }

        Usuario propietario = uRepository.findById(propietarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Propietario no encontrado con id: " + propietarioId));

        mascota.setPropietario(propietario);
        return mRepository.save(mascota);
    }

    // Obtener una mascota por su ID (HU04)
    @Override
    public Mascota getMascotaById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        return mRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada con id: " + id));
    }

    // Listar las mascotas de un propietario (HU04)
    @Override
    public List<Mascota> getMascotasPorPropietario(long propietarioId) {
        return mRepository.findByPropietarioId(propietarioId);
    }

    // Listar todas las mascotas del sistema (para el administrador)
    @Override
    public List<Mascota> getTodasMascotas() {
        return mRepository.findAll();
    }

    // Actualización parcial: solo sobrescribe los campos que vienen con valor
    @Override
    public Mascota actualizarMascota(Mascota mascota) {
        if (mascota == null || mascota.getId() == 0) {
            throw new IllegalArgumentException("La mascota debe tener un ID válido");
        }

        Mascota existente = mRepository.findById(mascota.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada con id: " + mascota.getId()));

        if (mascota.getNombre() != null && !mascota.getNombre().isBlank()) {
            existente.setNombre(mascota.getNombre());
        }
        if (mascota.getEspecie() != null && !mascota.getEspecie().isBlank()) {
            existente.setEspecie(mascota.getEspecie());
        }
        if (mascota.getRaza() != null) {
            existente.setRaza(mascota.getRaza());
        }
        if (mascota.getFechaNacimiento() != null) {
            existente.setFechaNacimiento(mascota.getFechaNacimiento());
        }
        if (mascota.getSexo() != null && !mascota.getSexo().isBlank()) {
            existente.setSexo(mascota.getSexo());
        }
        if (mascota.getPeso() != null) {
            existente.setPeso(mascota.getPeso());
        }
        if (mascota.getFoto() != null && !mascota.getFoto().isBlank()) {
            existente.setFoto(mascota.getFoto());
        }

        return mRepository.save(existente);
    }

    // Eliminar una mascota (también elimina su historial y citas por el cascade)
    @Override
    public void eliminarMascota(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        if (!mRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("No se puede eliminar. Mascota no encontrada con id: " + id);
        }
        mRepository.deleteById(id);
    }
}
