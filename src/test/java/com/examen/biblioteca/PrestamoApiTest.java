package com.examen.biblioteca;

import com.examen.biblioteca.entity.EstadoUsuario;
import com.examen.biblioteca.repository.PrestamoRepository;
import com.examen.biblioteca.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PrestamoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private String tokenAdmin;
    private String tokenBibliotecario;
    private String tokenLector;
    private long lectorId;

    @BeforeAll
    void preparar() throws Exception {
        tokenAdmin = login("admin@biblioteca.com", "Admin123*");
        tokenBibliotecario = login("bibliotecario@biblioteca.com", "Bibliotecario123*");
        JsonNode lector = loginCompleto("lector@biblioteca.com", "Lector123*");
        tokenLector = lector.get("token").asText();
        lectorId = lector.get("id").asLong();
    }

    private JsonNode loginCompleto(String email, String password) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    private String login(String email, String password) throws Exception {
        return loginCompleto(email, password).get("token").asText();
    }

    private long registrarLector(String prefijo) throws Exception {
        String email = prefijo + "." + System.nanoTime() + "@pruebas.com";
        String json = objectMapper.createObjectNode()
                .put("nombre", "Lector " + prefijo)
                .put("email", email)
                .put("password", "Secreta123")
                .toString();
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private long crearLibro(String titulo, int stockTotal, int stockDisponible) throws Exception {
        String isbn = "978-7-77-" + System.nanoTime() + "-1";
        MvcResult resultado = mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"" + isbn + "\",\"titulo\":\"" + titulo + "\",\"autor\":\"Autor\","
                                + "\"categoria\":\"Prestamo\",\"stockTotal\":" + stockTotal
                                + ",\"stockDisponible\":" + stockDisponible + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private JsonNode crearPrestamo(long usuarioId, long libroId) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + usuarioId + ",\"libroId\":" + libroId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    private int stockDe(long libroId) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v1/libros/{id}", libroId)
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("stockDisponible").asInt();
    }

    @Test
    void flujoCompletoDePrestamoYDevolucion() throws Exception {
        long libroId = crearLibro("Flujo Completo", 3, 3);

        // 1. Crear prestamo
        JsonNode prestamo = crearPrestamo(lectorId, libroId);
        assertEquals("ACTIVO", prestamo.get("estado").asText());
        assertNotNull(prestamo.get("fechaPrestamo").asText());
        LocalDate esperada = LocalDate.parse(prestamo.get("fechaDevolucionEsperada").asText());
        LocalDate pedida = LocalDate.parse(prestamo.get("fechaPrestamo").asText());
        assertEquals(14, java.time.temporal.ChronoUnit.DAYS.between(pedida, esperada),
                "el prestamo debe durar 14 dias");
        long prestamoId = prestamo.get("id").asLong();

        // 2. Stock decrementado en 1
        assertEquals(2, stockDe(libroId));

        // 3. Mis prestamos
        mockMvc.perform(get("/api/v1/prestamos/mis-prestamos")
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.id == " + prestamoId + ")]", hasSize(1)));

        // 4. Devolver
        mockMvc.perform(patch("/api/v1/prestamos/{id}/devolucion", prestamoId)
                        .header("Authorization", "Bearer " + tokenBibliotecario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("DEVUELTO")))
                .andExpect(jsonPath("$.fechaDevolucionReal").isNotEmpty());

        // 5. Stock recuperado
        assertEquals(3, stockDe(libroId));

        // 6. Devolucion repetida rechazada y stock estable
        mockMvc.perform(patch("/api/v1/prestamos/{id}/devolucion", prestamoId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isConflict());

        assertEquals(3, stockDe(libroId), "la devolucion repetida no debe volver a subir el stock");
    }

    @Test
    void bibliotecarioPuedeCrearPrestamos() throws Exception {
        long libroId = crearLibro("Bibliotecario Crea", 2, 2);
        long usuarioId = registrarLector("biblio.lector");

        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenBibliotecario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + usuarioId + ",\"libroId\":" + libroId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado", is("ACTIVO")));

        assertEquals(1, stockDe(libroId));
    }

    @Test
    void lectorNoPuedeCrearPrestamos() throws Exception {
        long libroId = crearLibro("Lector No Crea", 1, 1);

        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenLector)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + lectorId + ",\"libroId\":" + libroId + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void libroSinStockDevuelve409() throws Exception {
        long libroId = crearLibro("Sin Stock", 0, 0);

        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + lectorId + ",\"libroId\":" + libroId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("stock")));

        assertEquals(0, stockDe(libroId));
    }

    @Test
    void usuarioInexistenteDevuelve404() throws Exception {
        long libroId = crearLibro("Usuario Inexistente", 1, 1);

        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":999999,\"libroId\":" + libroId + "}"))
                .andExpect(status().isNotFound());

        assertEquals(1, stockDe(libroId), "el stock no debe modificarse si la operacion falla");
    }

    @Test
    void libroInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + lectorId + ",\"libroId\":999999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void datosInvalidosDevuelven400() throws Exception {
        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":-1,\"libroId\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.usuarioId").exists())
                .andExpect(jsonPath("$.fields.libroId").exists());
    }

    @Test
    void maximoTresPrestamosActivosParaLector() throws Exception {
        long usuarioId = registrarLector("max.tres");
        long libro1 = crearLibro("Max Uno", 1, 1);
        long libro2 = crearLibro("Max Dos", 1, 1);
        long libro3 = crearLibro("Max Tres", 1, 1);
        long libro4 = crearLibro("Max Cuatro", 1, 1);

        crearPrestamo(usuarioId, libro1);
        crearPrestamo(usuarioId, libro2);
        crearPrestamo(usuarioId, libro3);

        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + usuarioId + ",\"libroId\":" + libro4 + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("maximo")));

        assertEquals(1, stockDe(libro4), "el cuarto prestamo no debe tomar el ejemplar");

        // al devolver uno, vuelve a poder pedir
        long prestamoId = prestamoRepository.findByUsuarioIdOrderByFechaPrestamoDesc(usuarioId).get(0).getId();
        mockMvc.perform(patch("/api/v1/prestamos/{id}/devolucion", prestamoId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + usuarioId + ",\"libroId\":" + libro4 + "}"))
                .andExpect(status().isCreated());
    }

    @Test
    void prestamoAtrasadoSancionaAlLectorYBloqueaNuevosPrestamos() throws Exception {
        long usuarioId = registrarLector("atraso.demo");
        long libroA = crearLibro("Atraso Uno", 2, 2);
        long libroB = crearLibro("Atraso Dos", 2, 2);

        JsonNode prestamo = crearPrestamo(usuarioId, libroA);

        // se vence el prestamo (fecha esperada ayer)
        var entidad = prestamoRepository.findById(prestamo.get("id").asLong()).orElseThrow();
        entidad.setFechaDevolucionEsperada(LocalDate.now().minusDays(1));
        prestamoRepository.save(entidad);

        // el lector no puede pedir otro libro: se sanciona y se rechaza
        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + usuarioId + ",\"libroId\":" + libroB + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("sancionado")));

        assertEquals(EstadoUsuario.SANCIONADO,
                usuarioRepository.findById(usuarioId).orElseThrow().getEstado(),
                "el usuario debe quedar SANCIONADO");

        assertEquals(2, stockDe(libroB), "no se debe modificar el stock al rechazar");

        // el usuario sancionado no puede realizar nuevos prestamos
        long libroC = crearLibro("Atraso Tres", 2, 2);
        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + usuarioId + ",\"libroId\":" + libroC + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("sancionado")));

        assertEquals(2, stockDe(libroC));

        // el prestamo atrasado aparece en la lista de atrasados
        mockMvc.perform(get("/api/v1/prestamos/atrasados")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + prestamo.get("id").asLong() + ")]", hasSize(1)));

        // un lector no puede consultar los atrasados
        mockMvc.perform(get("/api/v1/prestamos/atrasados")
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isForbidden());
    }

    @Test
    void devolucionConPrestamoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(patch("/api/v1/prestamos/{id}/devolucion", 999999)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void devolucionConIdNoNumericoDevuelve400() throws Exception {
        mockMvc.perform(patch("/api/v1/prestamos/{id}/devolucion", "abc")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void solicitudesRepetidasNoRompenLaAplicacion() throws Exception {
        long libroId = crearLibro("Repetido", 1, 1);
        long usuarioId = registrarLector("repetido.user");

        // solo el primer intento toma el unico ejemplar
        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":" + usuarioId + ",\"libroId\":" + libroId + "}"))
                .andExpect(status().isCreated());

        // ya no queda stock: todo intento posterior falla sin caerse
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/prestamos")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"usuarioId\":" + usuarioId + ",\"libroId\":" + libroId + "}"))
                    .andExpect(status().isConflict());
        }

        assertEquals(0, stockDe(libroId));
        assertEquals("ACTIVO", usuarioRepository.findById(usuarioId).orElseThrow().getEstado().name());
    }
}
