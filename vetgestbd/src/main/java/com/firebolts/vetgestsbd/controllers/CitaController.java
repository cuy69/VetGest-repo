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

import com.firebolts.vetgestsbd.dtos.CambiarEstadoCitaDto;
import com.firebolts.vetgestsbd.dtos.ReprogramarCitaDto;
import com.firebolts.vetgestsbd.dtos.SolicitarCitaDto;
import com.firebolts.vetgestsbd.exceptions.RecursoNoEncontradoException;
import com.firebolts.vetgestsbd.models.Cita;
import com.firebolts.vetgestsbd.models.EstadoCita;
import com.firebolts.vetgestsbd.models.Mascota;
import com.firebolts.vetgestsbd.models.Usuario;
import com.firebolts.vetgestsbd.repositories.MascotaRepository;
import com.firebolts.vetgestsbd.repositories.UsuarioRepository;
import com.firebolts.vetgestsbd.services.CitaService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:5173", "http://localhost:3000"})
@RequestMapping({"/api/citas", "/api/appointments", "/appointments"})
public class CitaController {

    private final CitaService cService;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;

    private static final Logger logger = LoggerFactory.getLogger(CitaController.class);

    public static Map<String, Object> toFrontendAppointment(Cita c) {
        if (c == null) return null;
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", c.getId());
        map.put("pet", MascotaController.toFrontendPet(c.getMascota()));
        map.put("clinic", ClinicaController.findClinic(1L));
        map.put("service", ServicioVeterinarioController.toFrontendService(c.getServicio(), 1L));

        String startsAt = "";
        if (c.getFecha() != null && c.getHora() != null) {
            startsAt = c.getFecha().toString() + "T" + c.getHora().toString() + "-05:00";
        }
        map.put("starts_at", startsAt);
        map.put("reason", c.getMotivo() != null ? c.getMotivo() : "");
        String status = "Pendiente";
        if (c.getEstado() != null) {
            if (c.getEstado().name().equalsIgnoreCase("CANCELADA")) {
                status = "Cancelada";
            } else {
                status = "Pendiente";
            }
        }
        map.put("status", status);
        map.put("estado", c.getEstado());
        map.put("created_at", "2026-01-01T00:00:00Z");
        map.put("updated_at", "2026-01-01T00:00:00Z");
        return map;
    }

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

    private boolean puedeAccederACita(Cita cita) {
        if (esAdmin()) {
            return true;
        }
        return getUsuarioAutenticado()
                .map(u -> cita.getMascota() != null 
                        && cita.getMascota().getPropietario() != null 
                        && cita.getMascota().getPropietario().getId() == u.getId())
                .orElse(false);
    }

    // ==============================================================
    // 1. Solicitar una nueva cita (HU12)
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @PostMapping
    public ResponseEntity<?> solicitarCita(@Valid @RequestBody SolicitarCitaDto dto, BindingResult result) {
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

            // Si es CLIENTE, validar que la mascota le pertenezca
            if (!esAdmin()) {
                Mascota mascota = mascotaRepository.findById(java.util.Objects.requireNonNull(dto.getMascotaId()))
                        .orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada con id: " + dto.getMascotaId()));
                if (mascota.getPropietario() == null || mascota.getPropietario().getId() != usuarioActual.get().getId()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("status", "error", "message", "No puedes solicitar una cita para una mascota que no te pertenece"));
                }
            }

            Cita cita = new Cita();
            cita.setFecha(dto.getFecha());
            cita.setHora(dto.getHora());
            cita.setMotivo(dto.getMotivo());

            Cita guardada = cService.solicitarCita(cita, dto.getMascotaId(), dto.getServicioId());
            return ResponseEntity.status(HttpStatus.CREATED).body(toFrontendAppointment(guardada));

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
            logger.error("Error al solicitar cita: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Error al procesar la cita: " + e.getMessage()));
        }
    }

    // ==============================================================
    // 2. Obtener cita por ID
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getCitaById(@PathVariable("id") Long id) {
        try {
            Cita cita = cService.getCitaById(id);
            if (!puedeAccederACita(cita)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("status", "error", "message", "No tienes permiso para ver esta cita"));
            }
            return ResponseEntity.ok(toFrontendAppointment(cita));
        } catch (RecursoNoEncontradoException rne) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", rne.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ==============================================================
    // 3. Listar citas del cliente autenticado
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @GetMapping("/mis-citas")
    public ResponseEntity<?> getMisCitas() {
        try {
            Optional<Usuario> usuarioActual = getUsuarioAutenticado();
            if (usuarioActual.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", "error", "message", "Usuario no autenticado"));
            }

            List<Cita> citas = cService.getCitasPorPropietario(usuarioActual.get().getId());
            return ResponseEntity.ok(citas.stream().map(CitaController::toFrontendAppointment).toList());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ==============================================================
    // 4. Listar todas las citas (Compatible con /appointments raíz)
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @GetMapping
    public ResponseEntity<?> getTodasCitas(@RequestParam(name = "estado", required = false) EstadoCita estado) {
        try {
            if (!esAdmin()) {
                return getMisCitas();
            }
            List<Cita> citas = (estado != null) ? cService.getCitasPorEstado(estado) : cService.getTodasCitas();
            return ResponseEntity.ok(citas.stream().map(CitaController::toFrontendAppointment).toList());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ==============================================================
    // 5. Listar citas por propietario
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @GetMapping("/propietario/{propietarioId}")
    public ResponseEntity<?> getCitasPorPropietario(@PathVariable("propietarioId") Long propietarioId) {
        try {
            if (!esAdmin()) {
                Optional<Usuario> actual = getUsuarioAutenticado();
                if (actual.isEmpty() || actual.get().getId() != propietarioId) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("status", "error", "message", "No tienes permiso para ver las citas de este usuario"));
                }
            }

            List<Cita> citas = cService.getCitasPorPropietario(propietarioId);
            if (citas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            return ResponseEntity.ok(citas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ==============================================================
    // 6. Listar citas por mascota
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @GetMapping("/mascota/{mascotaId}")
    public ResponseEntity<?> getCitasPorMascota(@PathVariable("mascotaId") Long mascotaId) {
        try {
            if (!esAdmin()) {
                Mascota mascota = mascotaRepository.findById(java.util.Objects.requireNonNull(mascotaId))
                        .orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada con id: " + mascotaId));
                Optional<Usuario> actual = getUsuarioAutenticado();
                if (actual.isEmpty() || mascota.getPropietario() == null || mascota.getPropietario().getId() != actual.get().getId()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("status", "error", "message", "No tienes permiso para ver las citas de esta mascota"));
                }
            }

            List<Cita> citas = cService.getCitasPorMascota(mascotaId);
            if (citas.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            return ResponseEntity.ok(citas);
        } catch (RecursoNoEncontradoException rne) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", rne.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ==============================================================
    // 7. Reprogramar cita
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @PutMapping("/{id}/reprogramar")
    public ResponseEntity<?> reprogramarCita(@PathVariable("id") Long id,
                                             @Valid @RequestBody ReprogramarCitaDto dto,
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

            Cita citaExistente = cService.getCitaById(id);
            if (!puedeAccederACita(citaExistente)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("status", "error", "message", "No tienes permiso para reprogramar esta cita"));
            }

            Cita reprogramada = cService.reprogramarCita(id, dto.getFecha(), dto.getHora());
            return ResponseEntity.ok(reprogramada);

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
            logger.error("Error al reprogramar cita {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Error al reprogramar cita: " + e.getMessage()));
        }
    }

    // ==============================================================
    // 8. Cambiar estado de la cita
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable("id") Long id,
                                           @Valid @RequestBody CambiarEstadoCitaDto dto,
                                           BindingResult result) {
        try {
            if (result.hasErrors()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", "error", "message", "El nuevo estado es requerido y válido"));
            }

            Cita citaExistente = cService.getCitaById(id);
            if (!puedeAccederACita(citaExistente)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("status", "error", "message", "No tienes permiso para modificar esta cita"));
            }

            // Restricción: Un CLIENTE solo puede solicitar la cancelación de su cita
            if (!esAdmin() && dto.getEstado() != EstadoCita.CANCELADA) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("status", "error", "message", "Los clientes únicamente tienen permitido cancelar citas"));
            }

            Cita actualizada = cService.cambiarEstado(id, dto.getEstado());
            return ResponseEntity.ok(actualizada);

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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Error al cambiar estado: " + e.getMessage()));
        }
    }

    // ==============================================================
    // 9. Cancelar cita (Atajo conveniente)
    // ==============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @PatchMapping({"/{id}/cancelar", "/{id}/cancel"})
    public ResponseEntity<?> cancelarCita(@PathVariable("id") Long id) {
        try {
            Cita citaExistente = cService.getCitaById(id);
            if (!puedeAccederACita(citaExistente)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("status", "error", "message", "No tienes permiso para cancelar esta cita"));
            }

            Cita cancelada = cService.cambiarEstado(id, EstadoCita.CANCELADA);
            return ResponseEntity.ok(toFrontendAppointment(cancelada));

        } catch (RecursoNoEncontradoException rne) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", rne.getMessage()));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "error", "message", ise.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Error al cancelar cita: " + e.getMessage()));
        }
    }
}

