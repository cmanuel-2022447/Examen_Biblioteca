package com.examen.biblioteca.security;

import com.examen.biblioteca.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_NOMBRE = "nombre";
    public static final String CLAIM_ESTADO = "estado";

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long expirationMs;

    private SecretKey clave() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 caracteres");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String generarToken(Usuario usuario) {
        return generarToken(usuario, expirationMs);
    }

    public String generarToken(Usuario usuario, long tiempoDeVidaMs) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + tiempoDeVidaMs);
        return Jwts.builder()
                .subject(usuario.getEmail())
                .id(String.valueOf(usuario.getId()))
                .claim(CLAIM_NOMBRE, usuario.getNombre())
                .claim(CLAIM_ROL, usuario.getRol().name())
                .claim(CLAIM_ESTADO, usuario.getEstado().name())
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(clave())
                .compact();
    }

    public Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(clave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extraerEmail(String token) {
        return extraerClaims(token).getSubject();
    }

    public String extraerRol(String token) {
        return extraerClaims(token).get(CLAIM_ROL, String.class);
    }

    public <T> T extraerCampo(String token, Function<Claims, T> resolver) {
        return resolver.apply(extraerClaims(token));
    }

    public boolean esValido(String token, String emailEsperado) {
        try {
            Claims claims = extraerClaims(token);
            return emailEsperado != null && emailEsperado.equals(claims.getSubject())
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
