package com.firebolts.vetgestsbd.apis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UsuarioEmail {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioEmail.class);

    @Value("${mailboxlayer.api.key}")
    private String apiKey;

    private final RestTemplate clienteRest = new RestTemplate();

    public boolean esEmailValido(String email) {
        String url = String.format(
            "https://apilayer.net/api/check?access_key=%s&email=%s",
            apiKey, email
        );

        try {
            UsuarioEmailResponse respuesta = clienteRest.getForObject(java.util.Objects.requireNonNull(url), UsuarioEmailResponse.class);

            if (respuesta == null) {
                logger.warn("Respuesta nula de la API MailboxLayer para el email: {}", email);
                return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
            }

            boolean valido = respuesta.isFormatoCorrecto() && respuesta.isDireccionExistente();
            logger.info(
                "Validación de email [{}]: formato={}, existencia={}",
                email, respuesta.isFormatoCorrecto(), respuesta.isDireccionExistente()
            );

            return valido;

        } catch (Exception e) {
            logger.warn("Error o servicio inaccesible en MailboxLayer [{}]. Validación local aplicada: {}", email, e.getMessage());
            return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
        }
    }
}