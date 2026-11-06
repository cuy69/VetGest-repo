package com.firebolts.vetgestsbd.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class JwtUtil {

    // Clave secreta codificada en Base64 para firmar y verificar tokens
    private final String SECRET_KEY = "bCXu4Wg02Q4ck0+yHcfTeWNTK3AbSahCoOBtkVJ7joA3Qu71sULd3mGj2mqGNEccYUBKqzaQkQ9k4LwYBm7FdQ==";

    // Convierte la cadena Base64 en una clave secreta válida para HMAC
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Genera un token con usuario y roles
    public String generateToken(String username, Set<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles); // Se almacena "roles" (plural)

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hora
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
    return Jwts.parser()
           .verifyWith(getSigningKey())
           .build()
           .parseSignedClaims(token)
           .getPayload();
    }


    // Extrae el username
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Extrae los roles en plural
    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(String token) {
        return (Set<String>) extractAllClaims(token).get("roles");
    }

    // Verifica si el token expiró
    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    // VALIDA TOKEN para UserDetails (para JwtRequestFilter)
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
}
