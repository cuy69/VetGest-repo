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

import com.firebolts.vetgestsbd.dtos.RegistroHistorialDto;
import com.firebolts.vetgestsbd.exceptions.RecursoNoEncontradoException;
import com.firebolts.vetgestsbd.models.HistorialVeterinario;
import com.firebolts.vetgestsbd.models.Usuario;
import com.firebolts.vetgestsbd.repositories.MascotaRepository;
import com.firebolts.vetgestsbd.repositories.UsuarioRepository;
import com.firebolts.vetgestsbd.services.HistorialVeterinarioService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:5173", "http://localhost:3000"})
@RequestMapping("/api/historiales")
public class HistorialVeterinarioController {

    private final HistorialVeterinarioService hService;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;

    private static final Logger logger = LoggerFactory.getLogger(HistorialVeterinarioController.class);

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

    private boolean puedeAccederAMascota(long mascotaId) {
        if (esAdmin()) {
            return true;
        }
        Optional<Usuario> actual = getUsuarioAutenticado();
        if (actual.isEmpty()) {
            return false;
        }
        return mascotaRepository.findById(mascotaId)
                .map(m -> m.getPropietario() != null && m.getPropietario().getId() == actual.get().getId())
                .orElse(false);
    }

    // ==============================================================
    // 1. Registrar atención médica en historial (Solo ADMIN / Veterinario)
    // ==============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> registrarHistorial(@Valid @RequestBody RegistroHistorialDto dto,
                                                BindingResult result) {
        try {
            if (result.hasErrors()) {
                String errores = result.getAllErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Error en los datos ingresados");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", "error", "message", errores));
            }

            HistorialVeterinario historial = new HistorialVeterinario();
            historial.setFecha(dto.getFecha());
            historial.setDiagnostico(dto.getDiagnostico());
            historial.setTratamiento(dto.getTratamiento());
            historial.setObservaciones(dto.getObservaciones());
            historial.setPeso(dto.getPeso());

            HistorialVeterinario guardado = hService.registrarHistorial(historial, dto.getMascotaId(), dto.getCitaId());
            return ResponseEntity.status(HttpStatus.CREATED).body(guardado);

        } catch (RecursoNoEncontradoException rne) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", rne.getMessage()));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "error", "message", ise.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", iae.getMessage()));
        } catch (Exception e) {
            logger.error("Error al registrar historial: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Error al registrar historial: " + e.getMessage()));
        }
    }

    // ==============================================================
    // 2. Obtener un registro de historial por ID
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getHistorialById(@PathVariable("id") Long id) {
        try {
            HistorialVeterinario historial = hService.getHistorialById(id);
            if (!esAdmin()) {
                Optional<Usuario> actual = getUsuarioAutenticado();
                if (actual.isEmpty() 
                        || historial.getMascota() == null 
                        || historial.getMascota().getPropietario() == null 
                        || historial.getMascota().getPropietario().getId() != actual.get().getId()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("status", "error", "message", "No tienes permiso para ver este registro médico"));
                }
            }
            return ResponseEntity.ok(historial);

        } catch (RecursoNoEncontradoException rne) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", rne.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ==============================================================
    // 3. Historial médico completo de una mascota (HU03)
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @GetMapping("/mascota/{mascotaId}")
    public ResponseEntity<?> getHistorialPorMascota(@PathVariable("mascotaId") Long mascotaId) {
        try {
            if (!puedeAccederAMascota(mascotaId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("status", "error", "message", "No tienes permiso para ver el historial de esta mascota"));
            }

            List<HistorialVeterinario> historiales = hService.getHistorialPorMascota(mascotaId);
            if (historiales.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            return ResponseEntity.ok(historiales);

        } catch (RecursoNoEncontradoException rne) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", rne.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ==============================================================
    // 4. Actualizar registro de historial (Solo ADMIN / Veterinario)
    // ==============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarHistorial(@PathVariable("id") Long id,
                                                 @RequestBody HistorialVeterinario historial) {
        try {
            if (historial == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", "error", "message", "Request body vacío"));
            }

            historial.setId(id);
            HistorialVeterinario actualizado = hService.actualizarHistorial(historial);
            return ResponseEntity.ok(actualizado);

        } catch (RecursoNoEncontradoException rne) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", rne.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", iae.getMessage()));
        } catch (Exception e) {
            logger.error("Error al actualizar historial {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Error al actualizar historial: " + e.getMessage()));
        }
    }
}

