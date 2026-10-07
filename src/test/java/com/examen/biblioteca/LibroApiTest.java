package com.examen.biblioteca;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LibroApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenAdmin;
    private String tokenLector;
    private String tokenBibliotecario;

    @BeforeAll
    void prepararTokens() throws Exception {
        tokenAdmin = login("admin@biblioteca.com", "Admin123*");
        tokenBibliotecario = login("bibliotecario@biblioteca.com", "Bibliotecario123*");
        tokenLector = login("lector@biblioteca.com", "Lector123*");
    }

    private String login(String email, String password) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
    }

    private String cuerpoLibro(String isbn, String titulo, String categoria, int stockTotal, int stockDisponible) {
        return "{\"isbn\":\"" + isbn + "\",\"titulo\":\"" + titulo + "\",\"autor\":\"Autor Demo\","
                + "\"categoria\":\"" + categoria + "\",\"stockTotal\":" + stockTotal
                + ",\"stockDisponible\":" + stockDisponible + "}";
    }

    private long crearLibro(String isbn, String titulo, String categoria, int stockTotal, int stockDisponible)
            throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro(isbn, titulo, categoria, stockTotal, stockDisponible)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void listarSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/libros")).andExpect(status().isUnauthorized());
    }

    @Test
    void listarConTokenInvalidoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/libros")
                        .header("Authorization", "Bearer token-falso-12345"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("invalido")));
    }

    @Test
    void lectorNoPuedeCrearLibros() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenLector)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro("978-0-00-000000-1", "No Permitido", "Test", 1, 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void bibliotecarioNoPuedeCrearLibros() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenBibliotecario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro("978-0-00-000000-2", "No Permitido 2", "Test", 1, 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreaLibroYSePuedeConsultar() throws Exception {
        long id = crearLibro("978-0-45-" + System.nanoTime() + "-5", "Libro Creable", "Test", 5, 5);

        mockMvc.perform(get("/api/v1/libros/{id}", id)
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) id)))
                .andExpect(jsonPath("$.titulo", is("Libro Creable")))
                .andExpect(jsonPath("$.stockDisponible", is(5)));
    }

    @Test
    void isbnDuplicadoDevuelve409() throws Exception {
        String isbn = "978-0-45-" + System.nanoTime() + "-6";
        crearLibro(isbn, "Original", "Test", 2, 2);

        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro(isbn, "Copia", "Test", 1, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("ISBN")));
    }

    @Test
    void stockNegativoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro("978-0-45-" + System.nanoTime() + "-7", "Negativo", "Test", -1, -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields").exists());
    }

    @Test
    void stockDisponibleMayorQueTotalDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro("978-0-45-" + System.nanoTime() + "-8", "Imposible", "Test", 2, 5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("mayor al stock total")));
    }

    @Test
    void camposVaciosDevuelven400() throws Exception {
        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"\",\"titulo\":\"\",\"autor\":\"\",\"categoria\":\"\",\"stockTotal\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.isbn").exists())
                .andExpect(jsonPath("$.fields.titulo").exists());
    }

    @Test
    void libroInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/libros/{id}", 999999)
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isNotFound());
    }

    @Test
    void idNegativoDevuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/libros/{id}", -5)
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isNotFound());
    }

    @Test
    void idNoNumericoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/libros/{id}", "abc")
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filtrarPorTituloYCategoria() throws Exception {
        String sufijo = String.valueOf(System.nanoTime());
        crearLibro("978-1-11-" + sufijo + "-1", "Titulo Filtro " + sufijo, "CategoriaFiltro" + sufijo, 2, 2);

        mockMvc.perform(get("/api/v1/libros")
                        .param("titulo", "Filtro " + sufijo)
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[0].titulo", containsString("Titulo Filtro")));

        mockMvc.perform(get("/api/v1/libros")
                        .param("categoria", "CategoriaFiltro" + sufijo)
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))));
    }

    @Test
    void paginacionConParametrosInvalidosNoRompe() throws Exception {
        mockMvc.perform(get("/api/v1/libros")
                        .param("page", "-3")
                        .param("size", "-100")
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/libros")
                        .param("page", "0")
                        .param("size", "999999")
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(org.hamcrest.Matchers.lessThanOrEqualTo(100))));
    }

    @Test
    void actualizarLibroComoAdmin() throws Exception {
        long id = crearLibro("978-0-45-" + System.nanoTime() + "-9", "Para Actualizar", "Test", 3, 3);

        mockMvc.perform(put("/api/v1/libros/{id}", id)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro("978-0-45-" + System.nanoTime() + "-9", "Actualizado", "Test", 10, 7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo", is("Actualizado")))
                .andExpect(jsonPath("$.stockTotal", is(10)))
                .andExpect(jsonPath("$.stockDisponible", is(7)));
    }

    @Test
    void lectorNoPuedeActualizarNiEliminar() throws Exception {
        long id = crearLibro("978-0-45-" + System.nanoTime() + "-10", "Protegido", "Test", 1, 1);

        mockMvc.perform(put("/api/v1/libros/{id}", id)
                        .header("Authorization", "Bearer " + tokenLector)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibro("978-0-45-1-10", "Hackeado", "Test", 1, 1)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/libros/{id}", id)
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isForbidden());
    }

    @Test
    void eliminarLibroInexistenteDevuelve404() throws Exception {
        mockMvc.perform(delete("/api/v1/libros/{id}", 999999)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarLibroValidoDevuelve204() throws Exception {
        long id = crearLibro("978-0-45-" + System.nanoTime() + "-11", "Para Borrar", "Test", 1, 1);

        mockMvc.perform(delete("/api/v1/libros/{id}", id)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/libros/{id}", id)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void jsonConPropiedadesDesconocidasNoRompe() throws Exception {
        String email = "desconocidas." + System.nanoTime() + "@pruebas.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"email\":\"" + email + "\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Secreta123\",\"rol\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(login.getResponse().getContentAsString());
        assertTrue(json.get("token").asText().length() > 10);
    }
}
