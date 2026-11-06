package com.firebolts.vetgestsbd.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:5173", "http://localhost:3000"})
@RequestMapping({"/api/clinicas", "/api/clinics", "/clinics"})
public class ClinicaController {

    private static final List<Map<String, Object>> CLINICAS = new ArrayList<>();

    static {
        Map<String, Object> c1 = new HashMap<>();
        c1.put("id", 1L);
        c1.put("name", "VetGest Miraflores");
        c1.put("address", "Av. de la Salud 245, Miraflores, Lima");
        c1.put("phone", "01 555 0101");
        c1.put("opening_hours", "Lunes a sábado, 08:00–20:00");
        c1.put("description", "Atención cercana para perros y gatos. Un equipo dedicado a acompañarte en cada etapa de su vida.");
        CLINICAS.add(c1);

        Map<String, Object> c2 = new HashMap<>();
        c2.put("id", 2L);
        c2.put("name", "Huella Animal");
        c2.put("address", "Calle Los Olivos 180, San Borja, Lima");
        c2.put("phone", "01 555 0102");
        c2.put("opening_hours", "Lunes a sábado, 09:00–19:00");
        c2.put("description", "Consultas preventivas, orientación nutricional y cuidados con tiempo para escuchar tus dudas.");
        CLINICAS.add(c2);

        Map<String, Object> c3 = new HashMap<>();
        c3.put("id", 3L);
        c3.put("name", "Casa Mascota");
        c3.put("address", "Av. Los Jardines 620, Surco, Lima");
        c3.put("phone", "01 555 0103");
        c3.put("opening_hours", "Lunes a viernes, 08:00–18:00; sábado, 09:00–13:00");
        c3.put("description", "Un espacio tranquilo para la salud de tus compañeros, con atención general y controles periódicos.");
        CLINICAS.add(c3);
    }

    public static Map<String, Object> findClinic(Long id) {
        if (id == null) return CLINICAS.get(0);
        return CLINICAS.stream()
                .filter(c -> c.get("id").equals(id))
                .findFirst()
                .orElse(CLINICAS.get(0));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getClinicas(@RequestParam(name = "search", required = false) String search) {
        if (search == null || search.isBlank()) {
            return ResponseEntity.ok(CLINICAS);
        }
        String term = search.trim().toLowerCase();
        List<Map<String, Object>> filtered = CLINICAS.stream()
                .filter(c -> ((String) c.get("name")).toLowerCase().contains(term)
                        || ((String) c.get("address")).toLowerCase().contains(term))
                .toList();
        return ResponseEntity.ok(filtered);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getClinicaById(@PathVariable("id") Long id) {
        return CLINICAS.stream()
                .filter(c -> c.get("id").equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

