package com.firebolts.vetgestsbd.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.firebolts.vetgestsbd.models.PasswordResetToken;
import com.firebolts.vetgestsbd.models.Usuario;
import com.firebolts.vetgestsbd.repositories.PasswordResetTokenRepository;
import com.firebolts.vetgestsbd.repositories.UsuarioRepository;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MINUTES = 5;

    // Almacena intentos por email: email -> {lastAttempt: timestamp, attempts: count}
    private final Map<String, Map<String, Object>> retryTracker = new ConcurrentHashMap<>();

    private final JavaMailSender mailSender; // opcional: si no está configurado, solo logueamos
    private final PasswordResetTokenRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;

    public PasswordResetService(@Autowired(required = false) JavaMailSender mailSender,
                                PasswordResetTokenRepository tokenRepository,
                                UsuarioRepository usuarioRepository) {
        this.mailSender = mailSender;
        this.tokenRepository = tokenRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public String createPasswordResetTokenForEmail(String email, String appUrl) {
        Optional<Usuario> userOpt = usuarioRepository.findByEmail(email);
        if (userOpt.isEmpty()) return null;

        // Verificar límite de reintentos
        if (!canRetryPasswordReset(email)) {
            logger.warn("Demasiados intentos de recuperación para {}", email);
            return null;
        }

        Usuario user = userOpt.get();

        // Invalidar tokens anteriores del usuario: solo el más reciente es válido
        tokenRepository.deleteByUsuarioId(user.getId());

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(1);

        PasswordResetToken prt = new PasswordResetToken(token, user, expiry);
        tokenRepository.save(prt);

        String resetLink = appUrl + "/reset-password?token=" + token;

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(user.getEmail());
                message.setSubject("Recuperación de contraseña - VetGestsbd");
                message.setText("Hola " + user.getNombre() + ",\n\nPara restablecer tu contraseña haz clic en el siguiente enlace:\n" + resetLink + "\n\nEste enlace expira en 1 hora.\n\nSi no solicitaste esto, ignora este correo.\n\nSaludos,\nVetGestsbd");
                mailSender.send(message);
                logger.info("Correo de restablecimiento enviado a {}", user.getEmail());
                recordRetryAttempt(email);
            } catch (Exception e) {
                logger.error("Error enviando correo a {}. Error: {}", user.getEmail(), e.getMessage());
                return null;
            }
        } else {
            logger.warn("JavaMailSender no configurado. Reset link: {}", resetLink);
            recordRetryAttempt(email);
        }

        return token;
    }

    private boolean canRetryPasswordReset(String email) {
        Map<String, Object> tracker = retryTracker.get(email);
        if (tracker == null) {
            return true;
        }

        long lastAttempt = (long) tracker.get("lastAttempt");
        int attempts = (int) tracker.get("attempts");

        long elapsedMinutes = (System.currentTimeMillis() - lastAttempt) / (1000 * 60);

        // Si han pasado más de RETRY_DELAY_MINUTES, reiniciar contador
        if (elapsedMinutes > RETRY_DELAY_MINUTES) {
            return true;
        }

        // Si aún hay intentos disponibles en la ventana de tiempo
        return attempts < MAX_RETRY_ATTEMPTS;
    }

    private void recordRetryAttempt(String email) {
        Map<String, Object> tracker = retryTracker.get(email);
        long now = System.currentTimeMillis();

        if (tracker == null) {
            tracker = new HashMap<>();
            tracker.put("attempts", 1);
        } else {
            int attempts = (int) tracker.get("attempts");
            long lastAttempt = (long) tracker.get("lastAttempt");
            long elapsedMinutes = (now - lastAttempt) / (1000 * 60);

            // Si pasaron más de RETRY_DELAY_MINUTES, reiniciar el contador
            tracker.put("attempts", elapsedMinutes > RETRY_DELAY_MINUTES ? 1 : attempts + 1);
        }

        tracker.put("lastAttempt", now);
        retryTracker.put(email, tracker);

        logger.info("Intento de recuperación registrado para {}: {}/{} intentos", email, tracker.get("attempts"), MAX_RETRY_ATTEMPTS);
    }

    public Optional<Usuario> validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> prtOpt = tokenRepository.findByToken(token);
        if (prtOpt.isEmpty()) return Optional.empty();
        PasswordResetToken prt = prtOpt.get();
        if (prt.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(prt);
            return Optional.empty();
        }
        return Optional.of(prt.getUsuario());
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword, PasswordEncoder encoder) {
        Optional<PasswordResetToken> prtOpt = tokenRepository.findByToken(token);
        if (prtOpt.isEmpty()) return false;
        PasswordResetToken prt = prtOpt.get();
        if (prt.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(prt);
            return false;
        }

        Usuario user = prt.getUsuario();
        user.setPassword(encoder.encode(newPassword));
        usuarioRepository.save(user);

        // Borrar todos los tokens del usuario, no solo el usado
        tokenRepository.deleteByUsuarioId(user.getId());

        // Limpiar tracker de reintentos
        retryTracker.remove(user.getEmail());

        return true;
    }
}