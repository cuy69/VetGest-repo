package com.firebolts.vetgestsbd.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.firebolts.vetgestsbd.dtos.MascotaDto;
import com.firebolts.vetgestsbd.exceptions.RecursoNoEncontradoException;
import com.firebolts.vetgestsbd.models.Mascota;
import com.firebolts.vetgestsbd.models.Usuario;
import com.firebolts.vetgestsbd.repositories.MascotaRepository;
import com.firebolts.vetgestsbd.repositories.UsuarioRepository;
import com.firebolts.vetgestsbd.services.MascotaService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:5173", "http://localhost:3000"})
@RequestMapping({"/api/mascotas", "/api/pets", "/pets"})
public class MascotaController {

    private final MascotaService mService;
    private final MascotaRepository mRepository;
    private final UsuarioRepository usuarioRepository;

    private static final Logger logger = LoggerFactory.getLogger(MascotaController.class);

    // ==============================================================
    // Métodos auxiliares de seguridad
    // ==============================================================

    private Optional<Usuario> getUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        return usuarioRepository.findByUsername(auth.getName());
    }

    private boolean esAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public static Map<String, Object> toFrontendPet(Mascota m) {
        if (m == null) return null;
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", m.getId());
        map.put("name", m.getNombre());
        map.put("nombre", m.getNombre());
        map.put("species", m.getEspecie());
        map.put("especie", m.getEspecie());
        map.put("breed", m.getRaza() != null && !m.getRaza().isBlank() ? m.getRaza() : "Mestizo");
        map.put("raza", m.getRaza() != null && !m.getRaza().isBlank() ? m.getRaza() : "Mestizo");
        String sex = m.getSexo() != null ? m.getSexo() : "Macho";
        if (sex.equalsIgnoreCase("macho")) sex = "Macho";
        else if (sex.equalsIgnoreCase("hembra")) sex = "Hembra";
        map.put("sex", sex);
        map.put("sexo", sex);
        map.put("birth_date", m.getFechaNacimiento() != null ? m.getFechaNacimiento().toString() : "2020-01-01");
        map.put("fechaNacimiento", m.getFechaNacimiento());
        map.put("weight", m.getPeso() != null ? m.getPeso() : 5.0);
        map.put("peso", m.getPeso());
        map.put("color", m.getFoto() != null && !m.getFoto().isBlank() ? m.getFoto() : "Marrón");
        map.put("notes", "");
        map.put("created_at", "2026-01-01T00:00:00Z");
        map.put("updated_at", "2026-01-01T00:00:00Z");
        return map;
    }

    private boolean puedeAccederAMascota(Mascota mascota) {
        if (esAdmin()) {
            return true;
        }
        return getUsuarioAutenticado()
                .map(u -> mascota.getPropietario() != null && mascota.getPropietario().getId() == u.getId())
                .orElse(false);
    }

    // ==============================================================
    // 1. Registrar mascota (HU06)
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @PostMapping
    public ResponseEntity<?> registrarMascota(@Valid @RequestBody MascotaDto dto, BindingResult result) {
        try {
            if (result.hasErrors()) {
                String errores = result.getAllErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Error en los datos ingresados");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", "error", "message", errores));
            }

            Optional<Usuario> usuarioActual = getUsuarioAutenticado();
            if (usuarioActual.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", "error", "message", "Usuario no autenticado"));
            }

            long propietarioId;
            if (esAdmin()) {
                if (dto.getPropietarioId() == null || dto.getPropietarioId() <= 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("status", "error", "message", "Como administrador debe indicar el propietarioId"));
                }
                propietarioId = dto.getPropietarioId();
            } else {
                // Cliente siempre registra para su propio usuario
                propietarioId = usuarioActual.get().getId();
            }

            Mascota mascota = new Mascota();
            mascota.setNombre(dto.getNombre());
            mascota.setEspecie(dto.getEspecie());
            mascota.setRaza(dto.getRaza());
            mascota.setFechaNacimiento(dto.getFechaNacimiento());
            mascota.setSexo(dto.getSexo());
            mascota.setPeso(dto.getPeso());
            mascota.setFoto(dto.getFoto());

            Mascota guardada = mService.registrarMascota(mascota, propietarioId);
            return ResponseEntity.status(HttpStatus.CREATED).body(toFrontendPet(guardada));

        } catch (RecursoNoEncontradoException rne) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", rne.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", iae.getMessage()));
        } catch (Exception e) {
            logger.error("Error al registrar mascota: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Error al registrar la mascota: " + e.getMessage()));
        }
    }

    // ==============================================================
    // 2. Obtener mascota por ID (HU04)
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getMascotaById(@PathVariable("id") Long id) {
        try {
            Mascota mascota = mService.getMascotaById(id);
            if (!puedeAccederAMascota(mascota)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("status", "error", "message", "No tienes permiso para ver esta mascota"));
            }
            return ResponseEntity.ok(toFrontendPet(mascota));
        } catch (RecursoNoEncontradoException rne) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", rne.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ==============================================================
    // 3. Listar mascotas del cliente autenticado (HU04)
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @GetMapping("/mis-mascotas")
    public ResponseEntity<?> getMisMascotas() {
        try {
            Optional<Usuario> usuarioActual = getUsuarioAutenticado();
            if (usuarioActual.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", "error", "message", "Usuario no autenticado"));
            }

            List<Mascota> mascotas = mService.getMascotasPorPropietario(usuarioActual.get().getId());
            return ResponseEntity.ok(mascotas.stream().map(MascotaController::toFrontendPet).toList());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ==============================================================
    // 4. Listar mascotas por propietario
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @GetMapping("/propietario/{propietarioId}")
    public ResponseEntity<?> getMascotasPorPropietario(@PathVariable("propietarioId") Long propietarioId) {
        try {
            if (!esAdmin()) {
                Optional<Usuario> actual = getUsuarioAutenticado();
                if (actual.isEmpty() || actual.get().getId() != propietarioId) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("status", "error", "message", "No tienes permiso para ver las mascotas de este propietario"));
                }
            }

            List<Mascota> mascotas = mService.getMascotasPorPropietario(propietarioId);
            return ResponseEntity.ok(mascotas.stream().map(MascotaController::toFrontendPet).toList());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ==============================================================
    // 5. Listar mascotas raíz (Compatible con /pets y /api/mascotas)
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @GetMapping
    public ResponseEntity<?> getMascotas() {
        try {
            if (esAdmin()) {
                List<Mascota> mascotas = mRepository.findAll();
                return ResponseEntity.ok(mascotas.stream().map(MascotaController::toFrontendPet).toList());
            } else {
                return getMisMascotas();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ==============================================================
    // 6. Actualizar datos de la mascota
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarMascota(@PathVariable("id") Long id,
                                               @RequestBody MascotaDto dto) {
        try {
            Mascota existente = mService.getMascotaById(id);
            if (!puedeAccederAMascota(existente)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("status", "error", "message", "No tienes permiso para modificar esta mascota"));
            }

            Mascota datosActualizar = new Mascota();
            datosActualizar.setId(id);
            datosActualizar.setNombre(dto.getNombre());
            datosActualizar.setEspecie(dto.getEspecie());
            datosActualizar.setRaza(dto.getRaza());
            datosActualizar.setFechaNacimiento(dto.getFechaNacimiento());
            datosActualizar.setSexo(dto.getSexo());
            datosActualizar.setPeso(dto.getPeso());
            datosActualizar.setFoto(dto.getFoto());

            Mascota actualizada = mService.actualizarMascota(datosActualizar);
            return ResponseEntity.ok(toFrontendPet(actualizada));

        } catch (RecursoNoEncontradoException rne) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", rne.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", iae.getMessage()));
        } catch (Exception e) {
            logger.error("Error al actualizar mascota {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Error al actualizar mascota: " + e.getMessage()));
        }
    }

    // ==============================================================
    // 7. Eliminar mascota
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarMascota(@PathVariable("id") Long id) {
        try {
            Mascota existente = mService.getMascotaById(id);
            if (!puedeAccederAMascota(existente)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("status", "error", "message", "No tienes permiso para eliminar esta mascota"));
            }

            mService.eliminarMascota(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Mascota eliminada correctamente"));

        } catch (RecursoNoEncontradoException rne) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", rne.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Error al eliminar mascota: " + e.getMessage()));
        }
    }
}

