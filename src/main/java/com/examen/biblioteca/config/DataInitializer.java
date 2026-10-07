package com.examen.biblioteca.config;

import com.examen.biblioteca.entity.EstadoUsuario;
import com.examen.biblioteca.entity.Libro;
import com.examen.biblioteca.entity.Rol;
import com.examen.biblioteca.entity.Usuario;
import com.examen.biblioteca.repository.LibroRepository;
import com.examen.biblioteca.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Datos iniciales de prueba. Solo se crean si no existen, por lo que puede
 * ejecutarse en cada arranque sin duplicar registros.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final LibroRepository libroRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           LibroRepository libroRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.libroRepository = libroRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        crearUsuarioSiNoExiste("Administrador", "admin@biblioteca.com", "Admin123*", Rol.ADMIN);
        crearUsuarioSiNoExiste("Bibliotecario", "bibliotecario@biblioteca.com", "Bibliotecario123*", Rol.BIBLIOTECARIO);
        crearUsuarioSiNoExiste("Lector", "lector@biblioteca.com", "Lector123*", Rol.LECTOR);
        crearUsuarioSiNoExiste("Lector Dos", "lector2@biblioteca.com", "Lector123*", Rol.LECTOR);

        if (libroRepository.count() == 0) {
            crearLibro("978-0-14-044913-6", "Cien anos de soledad", "Gabriel Garcia Marquez", "Novela", 3);
            crearLibro("978-0-14-118776-1", "El principito", "Antoine de Saint-Exupery", "Fabula", 1);
            crearLibro("978-0-45-152493-5", "1984", "George Orwell", "Distopia", 2);
            crearLibro("978-0-06-112008-4", "Matar a un ruiseñor", "Harper Lee", "Novela", 0);
            crearLibro("978-0-31-676948-0", "El guardian entre el centeno", "J.D. Salinger", "Novela", 4);
            crearLibro("978-84-376-0494-7", "Cronica de una muerte anunciada", "Gabriel Garcia Marquez", "Novela", 2);
            log.info("Se crearon 6 libros de prueba");
        }
    }

    private void crearUsuarioSiNoExiste(String nombre, String email, String password, Rol rol) {
        if (usuarioRepository.existsByEmail(email)) {
            return;
        }
        usuarioRepository.save(new Usuario(nombre, email, passwordEncoder.encode(password),
                EstadoUsuario.ACTIVO, rol));
        log.info("Usuario inicial creado: {} ({})", email, rol);
    }

    private void crearLibro(String isbn, String titulo, String autor, String categoria, int stock) {
        libroRepository.save(new Libro(isbn, titulo, autor, categoria, stock, stock));
    }
}
