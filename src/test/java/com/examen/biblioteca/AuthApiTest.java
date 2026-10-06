package com.examen.biblioteca;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String emailUnico() {
        return "auth." + System.nanoTime() + "@pruebas.com";
    }

    private String registrarJson(String nombre, String email, String password) {
        return "{\"nombre\":\"" + nombre + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private MvcResult registrar(String nombre, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrarJson(nombre, email, password)))
                .andReturn();
    }

    private String login(String email, String password) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    @Test
    void registroCreaLectorActivoYDevuelveToken() throws Exception {
        String email = emailUnico();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrarJson("Lector Prueba", email, "Secreta123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.rol", is("LECTOR")))
                .andExpect(jsonPath("$.estado", is("ACTIVO")))
                .andExpect(jsonPath("$.email", is(email)));
    }

    @Test
    void registroNuncaPermiteRolAdmin() throws Exception {
        String email = emailUnico();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Hacker\",\"email\":\"" + email + "\",\"password\":\"Secreta123\","
                                + "\"rol\":\"ADMIN\",\"estado\":\"ACTIVO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol", is("LECTOR")));
    }

    @Test
    void registroConEmailDuplicadoDevuelve409() throws Exception {
        String email = emailUnico();
        registrar("Repetido", email, "Secreta123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrarJson("Repetido2", email, "OtraClave123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("registrado")));
    }

    @Test
    void registroConDatosInvalidosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"email\":\"no-es-email\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.nombre").exists())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void loginCorrectoDevuelveTokenYDatos() throws Exception {
        String email = emailUnico();
        registrar("Login Ok", email, "Secreta123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo", is("Bearer")))
                .andExpect(jsonPath("$.rol", is("LECTOR")));
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        String email = emailUnico();
        registrar("Login Mal", email, "Secreta123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"clave-mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Credenciales")));
    }

    @Test
    void loginConEmailInexistenteRealDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"noexistente@pruebas.com\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordsNuncaSeDevuelvenEnLaRespuesta() throws Exception {
        String email = emailUnico();
        MvcResult registro = registrar("Sin Password", email, "Secreta123");
        String cuerpo = registro.getResponse().getContentAsString();
        assertFalse(cuerpo.contains("Secreta123"), "la respuesta no debe contener la password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist());

        String token = login(email, "Secreta123");
        assertEquals(false, token == null || token.isBlank());
    }
}
