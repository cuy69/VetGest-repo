package com.firebolts.vetgestsbd.controllers;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.firebolts.vetgestsbd.exceptions.RecursoNoEncontradoException;
import com.firebolts.vetgestsbd.models.ServicioVeterinario;
import com.firebolts.vetgestsbd.services.ServicioVeterinarioService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:5173", "http://localhost:3000"})
@RequestMapping({"/api/servicios", "/api/services", "/services"})
public class ServicioVeterinarioController {

    private final ServicioVeterinarioService svService;

    private static final Logger logger = LoggerFactory.getLogger(ServicioVeterinarioController.class);

    public static Map<String, Object> toFrontendService(ServicioVeterinario s, Long clinicId) {
        if (s == null) return null;
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", s.getId());
        map.put("name", s.getNombre());
        map.put("nombre", s.getNombre());
        map.put("description", s.getDescripcion() != null ? s.getDescripcion() : "");
        map.put("descripcion", s.getDescripcion());
        map.put("duration_minutes", s.getDuracionMinutos() != null ? s.getDuracionMinutos() : 30);
        map.put("duracionMinutos", s.getDuracionMinutos());
        map.put("price", s.getPrecio() != null ? s.getPrecio().doubleValue() : 0.0);
        map.put("precio", s.getPrecio());
        map.put("clinic_id", clinicId != null ? clinicId : 1L);
        return map;
    }

    // ==============================================================
    // 1. Catálogo de servicios activos (HU13) - Acceso libre/clientes
    // ==============================================================

    @GetMapping
    public ResponseEntity<?> getServiciosActivos(@RequestParam(name = "clinic_id", required = false) Long clinicId) {
        try {
            List<ServicioVeterinario> servicios = svService.getServiciosActivos();
            return ResponseEntity.ok(servicios.stream().map(s -> toFrontendService(s, clinicId)).toList());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ==============================================================
    // 2. Todos los servicios, incluidos inactivos (Solo ADMIN)
    // ==============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/todos")
    public ResponseEntity<?> getTodosServicios() {
        try {
            List<ServicioVeterinario> servicios = svService.getTodosServicios();
            if (servicios.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            return ResponseEntity.ok(servicios);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ==============================================================
    // 3. Obtener servicio por ID
    // ==============================================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getServicioById(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(toFrontendService(svService.getServicioById(id), 1L));
        } catch (RecursoNoEncontradoException rne) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", rne.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ==============================================================
    // 4. Crear nuevo servicio veterinario (Solo ADMIN)
    // ==============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> crearServicio(@Valid @RequestBody ServicioVeterinario servicio, BindingResult result) {
        try {
            if (result.hasErrors()) {
                String errores = result.getAllErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Error en los datos ingresados");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", "error", "message", errores));
            }

            servicio.setId(0);
            ServicioVeterinario guardado = svService.crearServicio(servicio);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardado);

        } catch (IllegalStateException ise) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "error", "message", ise.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", iae.getMessage()));
        } catch (Exception e) {
            logger.error("Error al crear servicio: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Error al crear el servicio: " + e.getMessage()));
        }
    }

    // ==============================================================
    // 5. Actualizar servicio veterinario (Solo ADMIN)
    // ==============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarServicio(@PathVariable("id") Long id,
                                                @RequestBody ServicioVeterinario servicio) {
        try {
            if (servicio == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", "error", "message", "Request body vacío"));
            }
            servicio.setId(id);
            ServicioVeterinario actualizado = svService.actualizarServicio(servicio);
            return ResponseEntity.ok(actualizado);

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
            logger.error("Error al actualizar servicio {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Error al actualizar servicio: " + e.getMessage()));
        }
    }

    // ==============================================================
    // 6. Activar o desactivar servicio (Solo ADMIN)
    // ==============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable("id") Long id,
                                           @RequestParam("activo") boolean activo) {
        try {
            ServicioVeterinario actualizado = svService.cambiarEstado(id, activo);
            String estadoStr = activo ? "activado" : "desactivado";
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Servicio " + estadoStr + " correctamente",
                    "servicio", actualizado
            ));

        } catch (RecursoNoEncontradoException rne) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", rne.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Error al cambiar estado del servicio: " + e.getMessage()));
        }
    }
}

