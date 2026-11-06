package com.firebolts.vetgestsbd.controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.firebolts.vetgestsbd.apis.UsuarioEmail;
import com.firebolts.vetgestsbd.models.Rol;
import com.firebolts.vetgestsbd.models.RolTipo;
import com.firebolts.vetgestsbd.models.Usuario;
import com.firebolts.vetgestsbd.repositories.RolRepository;
import com.firebolts.vetgestsbd.repositories.UsuarioRepository;
import com.firebolts.vetgestsbd.services.UsuarioService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
// Permitir CORS desde dev servers (ajustar en producción)
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:5173", "http://localhost:3000"})
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService uService;
    private final UsuarioEmail usuarioEmail;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);

    // Indica si el usuario autenticado es ADMIN o es el dueño del perfil con ese id
    private boolean puedeAccederA(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (esAdmin) {
            return true;
        }
        return usuarioRepository.findByUsername(auth.getName())
                .map(u -> u.getId() == id)
                .orElse(false);
    }

    // ==============================================================
    // SOLO ADMIN puede crear usuarios directamente
    // ==============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> crearUsuario(@Valid @RequestBody Usuario usuario, BindingResult result) {
        try {
            if (result.hasErrors()) {
                String errores = result.getAllErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Error en los datos ingresados");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Error en los datos: " + errores);
            }

            usuario.setId(0L);

            if (!usuarioEmail.esEmailValido(usuario.getEmail())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Correo electrónico inválido o inexistente");
            }

            if (usuarioRepository.existsByUsername(usuario.getUsername())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("El nombre de usuario ya está en uso");
            }
            if (usuarioRepository.existsByEmail(usuario.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("El correo electrónico ya está registrado");
            }
            if (usuario.getDocumento() != null && usuarioRepository.existsByDocumento(usuario.getDocumento())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Ya existe un usuario con ese documento");
            }

            Rol rolCliente = rolRepository.findByRol(RolTipo.CLIENTE)
                    .orElseThrow(() -> new RuntimeException("Rol CLIENTE no encontrado"));
            usuario.setRoles(new HashSet<>(Set.of(rolCliente)));

            // Quita esta línea si la contraseña ya se encripta en otra capa
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

            Usuario guardado = uService.crearUsuario(usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardado);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear el usuario: " + e.getMessage());
        }
    }

    // ==============================================================
    // ADMIN puede ver cualquier perfil; CLIENTE solo el suyo
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getUsuarioById(@PathVariable("id") Long id) {
        try {
            if (!puedeAccederA(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("No tienes permiso para ver este perfil");
            }
            return ResponseEntity.ok(uService.getUsuarioById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Usuario no encontrado con ID: " + id);
        }
    }

    // ==============================================================
    // SOLO ADMIN puede listar todos los usuarios
    // ==============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<?> getTodosUsuarios() {
        try {
            List<Usuario> usuarios = uService.getTodosUsuarios();
            if (usuarios.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            return ResponseEntity.ok(usuarios);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al listar usuarios: " + e.getMessage());
        }
    }

    // ==============================================================
    // CLIENTE solo puede actualizar su propio perfil (HU05)
    // ADMIN puede actualizar cualquier usuario
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable("id") Long id,
                                               @RequestBody(required = false) Usuario usuario) {
        try {
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request body vacío o inválido");
            }
            if (!puedeAccederA(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("No tienes permiso para modificar este perfil");
            }

            logger.info("PUT /api/usuarios/{}", id);
            usuario.setId(id);

            // Evita que el usuario modifique su rol manualmente
            usuario.setRoles(null);

            // Quita este bloque si la contraseña ya se encripta en otra capa
            if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
                usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            }

            Usuario actualizado = uService.actualizarUsuario(usuario);
            logger.info("Usuario {} actualizado correctamente", id);
            return ResponseEntity.ok(actualizado);
        } catch (Exception e) {
            logger.error("Error al actualizar usuario {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar usuario: " + e.getMessage());
        }
    }

    // ==============================================================
    // SOLO ADMIN puede eliminar usuarios
    // ==============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable("id") Long id) {
        try {
            uService.eliminarUsuario(id);
            return ResponseEntity.ok(Map.of("message", "Usuario eliminado correctamente"));
        } catch (IllegalStateException ise) {
            // Por ejemplo: intento de eliminar el último ADMIN
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ise.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar usuario: " + e.getMessage());
        }
    }

    // ==============================================================
    // Eliminar la cuenta del usuario autenticado (CLIENTE o ADMIN)
    // ==============================================================

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/me")
    public ResponseEntity<?> eliminarMiCuenta() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            Optional<Usuario> maybe = usuarioRepository.findByUsername(username);
            if (maybe.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
            }

            uService.eliminarUsuario(maybe.get().getId());
            return ResponseEntity.ok(Map.of("message", "Cuenta eliminada correctamente"));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ise.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar cuenta: " + e.getMessage());
        }
    }

    // ==============================================================
    // CONSULTAS PERSONALIZADAS (solo ADMIN)
    // ==============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUsuarioByUsername(@PathVariable String username) {
        return usuarioRepository.findByUsername(username)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Usuario no encontrado con username: " + username));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUsuarioByEmail(@PathVariable String email) {
        return usuarioRepository.findByEmail(email)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Usuario no encontrado con email: " + email));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/rol/{rolTipo}")
    public ResponseEntity<?> getUsuariosPorRol(@PathVariable RolTipo rolTipo) {
        List<Usuario> usuarios = usuarioRepository.findByRolTipo(rolTipo);
        if (usuarios.isEmpty())
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        return ResponseEntity.ok(usuarios);
    }

    // Mascotas asociadas a los propietarios
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/con-mascotas")
    public ResponseEntity<?> getUsuariosConMascotas() {
        List<Usuario> usuarios = usuarioRepository.findUsuariosConMascotas();
        if (usuarios.isEmpty())
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        return ResponseEntity.ok(usuarios);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/buscar")
    public ResponseEntity<?> buscarUsuarios(@RequestParam String palabra) {
        List<Usuario> usuarios = usuarioRepository.buscarPorNombreOApellido(palabra);
        if (usuarios.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No se encontraron usuarios que coincidan con: " + palabra);
        return ResponseEntity.ok(usuarios);
    }

    // ==============================================================
    // Verificaciones de existencia (usadas por el formulario de registro)
    // ==============================================================

    @GetMapping("/existe/username/{username}")
    public ResponseEntity<?> existePorUsername(@PathVariable String username) {
        return ResponseEntity.ok(Map.of("exists", usuarioRepository.existsByUsername(username)));
    }

    @GetMapping("/existe/email/{email}")
    public ResponseEntity<?> existePorEmail(@PathVariable String email) {
        return ResponseEntity.ok(Map.of("exists", usuarioRepository.existsByEmail(email)));
    }

    // Variantes con query params para evitar problemas con caracteres especiales
    @GetMapping("/existe/username")
    public ResponseEntity<?> existePorUsernameQuery(@RequestParam(name = "username") String username) {
        return ResponseEntity.ok(Map.of("exists", usuarioRepository.existsByUsername(username)));
    }

    @GetMapping("/existe/email")
    public ResponseEntity<?> existePorEmailQuery(@RequestParam(name = "email") String email) {
        return ResponseEntity.ok(Map.of("exists", usuarioRepository.existsByEmail(email)));
    }
}