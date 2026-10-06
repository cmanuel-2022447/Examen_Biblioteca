package com.examen.biblioteca;

import com.examen.biblioteca.entity.EstadoUsuario;
import com.examen.biblioteca.entity.Rol;
import com.examen.biblioteca.entity.Usuario;
import com.examen.biblioteca.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private Usuario usuario(Rol rol) {
        return new Usuario("Usuario " + rol, rol.name().toLowerCase() + "@jwt.com", "sin-password",
                EstadoUsuario.ACTIVO, rol);
    }

    @Test
    void elTokenContieneElRolYElEmail() {
        String token = jwtService.generarToken(usuario(Rol.ADMIN));

        assertEquals("admin@jwt.com", jwtService.extraerEmail(token));
        assertEquals("ADMIN", jwtService.extraerRol(token));
        assertTrue(jwtService.esValido(token, "admin@jwt.com"));
    }

    @Test
    void unTokenExpiradoEsRechazado() {
        String token = jwtService.generarToken(usuario(Rol.LECTOR), -1000);

        assertThrows(ExpiredJwtException.class, () -> jwtService.extraerClaims(token));
        assertFalse(jwtService.esValido(token, "lector@jwt.com"), "un token expirado no es valido");
    }

    @Test
    void unTokenManipuladoEsRechazado() {
        String token = jwtService.generarToken(usuario(Rol.LECTOR));
        String tokenRoto = token.substring(0, token.length() - 3) + "abc";

        assertThrows(JwtException.class, () -> jwtService.extraerClaims(tokenRoto));
        assertFalse(jwtService.esValido(tokenRoto, "lector@jwt.com"));
    }

    @Test
    void unTokenNoEsValidoParaOtroUsuario() {
        String token = jwtService.generarToken(usuario(Rol.LECTOR));

        assertFalse(jwtService.esValido(token, "otro@jwt.com"));
    }

    @Test
    void cualquierCadenaBasuraNoEsUnToken() {
        assertFalse(jwtService.esValido("no-es-un-jwt", "lector@jwt.com"));
        assertThrows(JwtException.class, () -> jwtService.extraerClaims("no-es-un-jwt"));
    }
}
