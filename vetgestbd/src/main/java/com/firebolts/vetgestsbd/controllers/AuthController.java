package com.firebolts.vetgestsbd.controllers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.firebolts.vetgestsbd.apis.UsuarioEmail;
import com.firebolts.vetgestsbd.dtos.LoginDto;
import com.firebolts.vetgestsbd.dtos.RegisterDto;
import com.firebolts.vetgestsbd.models.Rol;
import com.firebolts.vetgestsbd.models.RolTipo;
import com.firebolts.vetgestsbd.models.Usuario;
import com.firebolts.vetgestsbd.repositories.RolRepository;
import com.firebolts.vetgestsbd.repositories.UsuarioRepository;
import com.firebolts.vetgestsbd.services.PasswordResetService;
import com.firebolts.vetgestsbd.services.UsuarioCustomerDetailsService;
import com.firebolts.vetgestsbd.services.UsuarioService;
import com.firebolts.vetgestsbd.utils.JwtUtil;

@RestController
@RequestMapping({"/auth", "/api/auth"})
public class AuthController {

    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final String MENSAJE_FORGOT = "Si el correo estÃ¡ registrado, recibirÃ¡s un enlace para restablecer tu contraseÃ±a";

    private final UsuarioCustomerDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioEmail usuarioEmail;
    private final PasswordResetService passwordResetService;

    // URL del frontend para los enlaces de recuperaciÃ³n. Se puede cambiar en
    // application.properties
    // con: app.frontend.url=https://tu-dominio.com
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public AuthController(UsuarioCustomerDetailsService userDetailsService,
            JwtUtil jwtUtil,
            UsuarioService usuarioService,
            PasswordEncoder passwordEncoder,
            RolRepository rolRepository,
            UsuarioRepository usuarioRepository,
            UsuarioEmail usuarioEmail,
            PasswordResetService passwordResetService) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioEmail = usuarioEmail;
        this.passwordResetService = passwordResetService;
    }

    // ==============================================================
    // Utilidades privadas
    // ==============================================================

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "error");
        resp.put("message", mensaje);
        return resp;
    }

    private boolean passwordInvalida(String password) {
        return password == null || password.length() < PASSWORD_MIN_LENGTH;
    }

    private String generarToken(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Set<String> authorities = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());
        return jwtUtil.generateToken(userDetails.getUsername(), authorities);
    }

    private Usuario construirUsuario(RegisterDto dto, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        
        String username = dto.getUsername();
        if (username == null || username.isBlank()) {
            if (dto.getEmail() != null && dto.getEmail().contains("@")) {
                username = dto.getEmail().split("@")[0];
            } else {
                username = dto.getNombre().toLowerCase().replaceAll("\\s+", "");
            }
            if (username.length() > 25) {
                username = username.substring(0, 25);
            }
            if (usuarioRepository.findByUsername(username).isPresent()) {
                username = username + (System.currentTimeMillis() % 1000);
            }
        }
        usuario.setUsername(username);
        usuario.setEmail(dto.getEmail());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setTelefono(dto.getTelefono());
        usuario.setDireccion(dto.getDireccion());
        usuario.setDocumento(dto.getDocumento());
        usuario.setRoles(new HashSet<>(Set.of(rol)));
        return usuario;
    }

    // ==============================================================
    // Registro (HU01)
    // ==============================================================

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterDto registerDto) {
        if (passwordInvalida(registerDto.getPassword())) {
            return error("La contraseña debe tener al menos " + PASSWORD_MIN_LENGTH + " caracteres");
        }

        // Validar email con MailboxLayer (o formato local en caso de fallo externo)
        if (!usuarioEmail.esEmailValido(registerDto.getEmail())) {
            return error("Correo inválido o inexistente");
        }

        try {
            Rol rolCliente = rolRepository.findByRol(RolTipo.CLIENTE)
                    .orElseThrow(() -> new RuntimeException("Rol CLIENTE no encontrado"));

            Usuario nuevoUsuario = usuarioService.crearUsuario(construirUsuario(registerDto, rolCliente));

            Map<String, Object> profile = new HashMap<>();
            profile.put("first_name", nuevoUsuario.getNombre());
            profile.put("last_name", nuevoUsuario.getApellido());
            profile.put("phone", nuevoUsuario.getTelefono() != null ? nuevoUsuario.getTelefono() : "");

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("token", generarToken(nuevoUsuario.getUsername()));
            response.put("id", nuevoUsuario.getId());
            response.put("email", nuevoUsuario.getEmail());
            response.put("profile", profile);
            return response;

        } catch (Exception e) {
            return error("Error al registrar usuario: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register-admin")
    public Map<String, Object> registerAdmin(@RequestBody RegisterDto registerDto) {
        if (passwordInvalida(registerDto.getPassword())) {
            return error("La contraseÃ±a debe tener al menos " + PASSWORD_MIN_LENGTH + " caracteres");
        }

        try {
            // Rol indicado en el DTO o CLIENTE por defecto
            RolTipo tipo = registerDto.getRol() != null ? registerDto.getRol() : RolTipo.CLIENTE;
            Rol rolAsignado = rolRepository.findByRol(tipo)
                    .orElseThrow(() -> new RuntimeException("Rol " + tipo + " no encontrado"));

            Usuario nuevoUsuario = usuarioService.crearUsuario(construirUsuario(registerDto, rolAsignado));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("token", generarToken(nuevoUsuario.getUsername()));
            return response;

        } catch (Exception e) {
            return error("Error al registrar usuario (ADMIN): " + e.getMessage());
        }
    }

    // ==============================================================
    // Login (HU02)
    // ==============================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto) {
        try {
            Optional<Usuario> usuarioOptional = usuarioRepository.findByEmail(loginDto.getEmail());
            if (usuarioOptional.isEmpty()) {
                usuarioOptional = usuarioRepository.findByUsername(loginDto.getEmail());
            }

            if (usuarioOptional.isEmpty()
                    || !passwordEncoder.matches(loginDto.getPassword(), usuarioOptional.get().getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", "error", "message", "Correo o contraseña inválidos"));
            }

            Usuario usuario = usuarioOptional.get();

            Map<String, Object> profile = new HashMap<>();
            profile.put("first_name", usuario.getNombre());
            profile.put("last_name", usuario.getApellido());
            profile.put("phone", usuario.getTelefono() != null ? usuario.getTelefono() : "");

            Map<String, Object> usuarioData = new HashMap<>();
            usuarioData.put("id", usuario.getId());
            usuarioData.put("nombre", usuario.getNombre());
            usuarioData.put("apellido", usuario.getApellido());
            usuarioData.put("username", usuario.getUsername());
            usuarioData.put("email", usuario.getEmail());
            usuarioData.put("telefono", usuario.getTelefono());
            usuarioData.put("direccion", usuario.getDireccion());
            usuarioData.put("documento", usuario.getDocumento());

            List<String> roles = usuario.getRoles().stream()
                    .map(r -> r.getRol().name())
                    .collect(Collectors.toList());
            usuarioData.put("roles", roles);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("token", generarToken(usuario.getUsername()));
            response.put("id", usuario.getId());
            response.put("email", usuario.getEmail());
            response.put("profile", profile);
            response.put("usuario", usuarioData);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Error en el login: " + e.getMessage()));
        }
    }

    // ==============================================================
    // Sesión actual y perfil (/auth/me, /auth/profile, /auth/logout)
    // ==============================================================

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autenticado"));
        }
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(auth.getName());
        if (usuarioOpt.isEmpty()) {
            usuarioOpt = usuarioRepository.findByEmail(auth.getName());
        }
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Usuario no encontrado"));
        }
        Usuario u = usuarioOpt.get();
        Map<String, Object> profile = new HashMap<>();
        profile.put("first_name", u.getNombre());
        profile.put("last_name", u.getApellido());
        profile.put("phone", u.getTelefono() != null ? u.getTelefono() : "");

        Map<String, Object> res = new HashMap<>();
        res.put("id", u.getId());
        res.put("email", u.getEmail());
        res.put("profile", profile);
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autenticado"));
        }
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(auth.getName());
        if (usuarioOpt.isEmpty()) {
            usuarioOpt = usuarioRepository.findByEmail(auth.getName());
        }
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Usuario no encontrado"));
        }
        Usuario u = usuarioOpt.get();
        if (body.containsKey("first_name") && body.get("first_name") != null) u.setNombre(body.get("first_name"));
        if (body.containsKey("last_name") && body.get("last_name") != null) u.setApellido(body.get("last_name"));
        if (body.containsKey("phone") && body.get("phone") != null) u.setTelefono(body.get("phone"));
        usuarioRepository.save(u);

        Map<String, Object> profile = new HashMap<>();
        profile.put("first_name", u.getNombre());
        profile.put("last_name", u.getApellido());
        profile.put("phone", u.getTelefono() != null ? u.getTelefono() : "");

        Map<String, Object> res = new HashMap<>();
        res.put("id", u.getId());
        res.put("email", u.getEmail());
        res.put("profile", profile);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("status", "success", "message", "Sesión cerrada correctamente"));
    }

    // ==============================================================
    // RecuperaciÃ³n de contraseÃ±a
    // ==============================================================

    // Siempre responde lo mismo, exista o no el correo, para no revelar quÃ© correos
    // estÃ¡n registrados
    private Map<String, Object> procesarForgot(String email) {
        if (email == null || email.isBlank()) {
            return error("Email requerido");
        }

        passwordResetService.createPasswordResetTokenForEmail(email, frontendUrl);

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "success");
        resp.put("message", MENSAJE_FORGOT);
        return resp;
    }

    private Map<String, Object> procesarReset(String token, String password) {
        if (token == null || token.isBlank() || passwordInvalida(password)) {
            return error("Token o contraseÃ±a invÃ¡lidos (mÃ­nimo " + PASSWORD_MIN_LENGTH + " caracteres)");
        }

        boolean ok = passwordResetService.resetPassword(token, password, passwordEncoder);
        if (!ok) {
            return error("Token invÃ¡lido o expirado");
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "success");
        resp.put("message", "ContraseÃ±a actualizada correctamente");
        return resp;
    }

    // Endpoint moderno: /auth/forgot
    @PostMapping("/forgot")
    public Map<String, Object> forgot(@RequestBody Map<String, String> body) {
        return procesarForgot(body.get("email"));
    }

    // Compatibilidad con endpoint antiguo /auth/forgot-password
    @PostMapping("/forgot-password")
    public Map<String, Object> forgotPasswordCompat(@RequestBody Map<String, String> payload) {
        return procesarForgot(payload.get("email"));
    }

    // Endpoint moderno: /auth/reset
    @PostMapping("/reset")
    public Map<String, Object> reset(@RequestBody Map<String, String> body) {
        return procesarReset(body.get("token"), body.get("password"));
    }

    // Compatibilidad con endpoint antiguo /auth/reset-password
    @PostMapping("/reset-password")
    public Map<String, Object> resetPasswordCompat(@RequestBody Map<String, String> payload) {
        return procesarReset(payload.get("token"), payload.get("password"));
    }

    // ==============================================================
    // EstadÃ­sticas
    // ==============================================================

    @GetMapping("/total-usuarios")
    public Map<String, Object> getTotalUsuarios() {
        try {
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            resp.put("total", usuarioRepository.count());
            return resp;
        } catch (Exception e) {
            return error("Error al obtener total de usuarios");
        }
    }
}
