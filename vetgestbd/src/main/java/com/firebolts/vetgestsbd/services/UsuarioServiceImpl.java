package com.firebolts.vetgestsbd.services;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.firebolts.vetgestsbd.exceptions.RecursoNoEncontradoException;
import com.firebolts.vetgestsbd.models.RolTipo;
import com.firebolts.vetgestsbd.models.Usuario;
import com.firebolts.vetgestsbd.repositories.PasswordResetTokenRepository;
import com.firebolts.vetgestsbd.repositories.UsuarioRepository;

@Service(value = "uService")
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository uRepository; // Inyección del repositorio
    private final PasswordResetTokenRepository tokenRepository; // Para limpiar tokens al eliminar un usuario
    private static final Logger logger = LoggerFactory.getLogger(UsuarioServiceImpl.class);

    public UsuarioServiceImpl(UsuarioRepository uRepository, PasswordResetTokenRepository tokenRepository) {
        this.uRepository = uRepository;
        this.tokenRepository = tokenRepository;
    }

    // Crear un nuevo usuario
    @Override
    public Usuario crearUsuario(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }
        // Verificar unicidad de username, email y documento antes de guardar
        if (usuario.getUsername() != null && uRepository.existsByUsername(usuario.getUsername())) {
            throw new IllegalStateException("El nombre de usuario ya está en uso");
        }
        if (usuario.getEmail() != null && uRepository.existsByEmail(usuario.getEmail())) {
            throw new IllegalStateException("El correo electrónico ya está registrado");
        }
        if (usuario.getDocumento() != null && uRepository.existsByDocumento(usuario.getDocumento())) {
            throw new IllegalStateException("Ya existe un usuario con ese documento");
        }

        return uRepository.save(usuario);
    }

    // Obtener un usuario por su ID
    @Override
    public Usuario getUsuarioById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }

        return uRepository.findById(id).orElseThrow(
                () -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + id));
    }

    // Obtener todos los usuarios registrados
    @Override
    public List<Usuario> getTodosUsuarios() {
        return uRepository.findAll();
    }

    // Actualizar un usuario existente
    @Override
    public Usuario actualizarUsuario(Usuario usuario) {
        Objects.requireNonNull(usuario, "El usuario no puede ser nulo");
        if (usuario.getId() == 0) {
            throw new IllegalArgumentException("El ID del usuario no puede ser 0");
        }

        Usuario existeUsuario = uRepository.findById(usuario.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + usuario.getId()));

        logger.info("Actualizando usuario id={}. Valores entrantes: nombre={}, apellido={}, email={}, username={}, telefonoPresent={}, direccionPresent={}, documentoPresent={}, profilePhotoPresent={}",
                usuario.getId(), usuario.getNombre(), usuario.getApellido(), usuario.getEmail(), usuario.getUsername(),
                usuario.getTelefono() != null, usuario.getDireccion() != null,
                usuario.getDocumento() != null, usuario.getProfilePhoto() != null);

        // Actualización parcial: solo sobrescribir cuando el cliente envía valores no nulos/no vacíos
        if (usuario.getNombre() != null && !usuario.getNombre().isBlank()) {
            existeUsuario.setNombre(usuario.getNombre());
        }
        if (usuario.getApellido() != null && !usuario.getApellido().isBlank()) {
            existeUsuario.setApellido(usuario.getApellido());
        }
        // Username: si cambia, no debe pertenecer a otro usuario
        if (usuario.getUsername() != null && !usuario.getUsername().isBlank()
                && !usuario.getUsername().equals(existeUsuario.getUsername())) {
            if (uRepository.existsByUsername(usuario.getUsername())) {
                throw new IllegalStateException("El nombre de usuario ya está en uso");
            }
            existeUsuario.setUsername(usuario.getUsername());
        }
        // Email: si cambia, no debe pertenecer a otro usuario
        if (usuario.getEmail() != null && !usuario.getEmail().isBlank()
                && !usuario.getEmail().equals(existeUsuario.getEmail())) {
            if (uRepository.existsByEmail(usuario.getEmail())) {
                throw new IllegalStateException("El correo electrónico ya está registrado");
            }
            existeUsuario.setEmail(usuario.getEmail());
        }
        // Solo actualizar la contraseña si el cliente envía un valor no vacío
        if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
            existeUsuario.setPassword(usuario.getPassword());
        }
        if (usuario.getTelefono() != null && !usuario.getTelefono().isBlank()) {
            existeUsuario.setTelefono(usuario.getTelefono());
        }
        if (usuario.getDireccion() != null && !usuario.getDireccion().isBlank()) {
            existeUsuario.setDireccion(usuario.getDireccion());
        }
        // Documento: el setter del model convierte vacío en null, y null significa "no modificar".
        // Se valida que no pertenezca a otro usuario.
        if (usuario.getDocumento() != null) {
            uRepository.findByDocumento(usuario.getDocumento()).ifPresent(otro -> {
                if (otro.getId() != existeUsuario.getId()) {
                    throw new IllegalStateException("Ya existe un usuario con ese documento");
                }
            });
            existeUsuario.setDocumento(usuario.getDocumento());
        }
        if (usuario.getProfilePhoto() != null && !usuario.getProfilePhoto().isBlank()) {
            existeUsuario.setProfilePhoto(usuario.getProfilePhoto());
        }

        // Solo se actualizan los roles si vienen explícitos (por un ADMIN)
        if (usuario.getRoles() != null && !usuario.getRoles().isEmpty()) {
            existeUsuario.setRoles(usuario.getRoles());
        }

        logger.debug("Guardando usuario id={}", existeUsuario.getId());

        return uRepository.save(existeUsuario);
    }

    // Eliminar un usuario por su ID
    @Override
    @Transactional
    public void eliminarUsuario(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }

        Usuario usuario = uRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se puede eliminar. Usuario no encontrado con id: " + id));

        // Comprobar si el usuario a eliminar tiene rol ADMIN y si es el último admin
        boolean esAdmin = usuario.getRoles() != null
                && usuario.getRoles().stream().anyMatch(r -> r.getRol() == RolTipo.ADMIN);

        if (esAdmin && uRepository.findByRolTipo(RolTipo.ADMIN).size() <= 1) {
            throw new IllegalStateException("No se puede eliminar el último administrador");
        }

        // Evita el error de FK con password_reset_tokens
        tokenRepository.deleteByUsuarioId(id);
        uRepository.deleteById(id);
    }
}