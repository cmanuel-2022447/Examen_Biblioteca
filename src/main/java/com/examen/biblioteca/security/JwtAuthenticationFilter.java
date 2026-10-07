package com.examen.biblioteca.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final JpaUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, JpaUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIJO_BEARER.length()).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtService.extraerEmail(token);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if (jwtService.esValido(token, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken autenticacion =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            }
        } catch (ExpiredJwtException ex) {
            request.setAttribute(JpaUserDetailsService.ATRIBUTO_ERROR_JWT, "Token expirado, inicie sesion nuevamente");
        } catch (JwtException | IllegalArgumentException ex) {
            request.setAttribute(JpaUserDetailsService.ATRIBUTO_ERROR_JWT, "Token invalido");
        } catch (UsernameNotFoundException ex) {
            request.setAttribute(JpaUserDetailsService.ATRIBUTO_ERROR_JWT, "Token invalido o usuario inexistente");
        } catch (Exception ex) {
            request.setAttribute(JpaUserDetailsService.ATRIBUTO_ERROR_JWT, "Token invalido");
        }

        filterChain.doFilter(request, response);
    }
}
