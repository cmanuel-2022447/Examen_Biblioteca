package com.examen.biblioteca;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba critica de concurrencia: dos solicitudes simultaneas sobre el ultimo
 * ejemplar de un libro. Solo una puede quedarse con el ejemplar y el stock
 * jamas puede quedar en negativo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrenciaTest {

    @LocalServerPort
    private int puerto;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    private String url(String path) {
        return "http://localhost:" + puerto + path;
    }

    private JsonNode login(String email, String password) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> respuesta = rest.postForEntity(url("/api/v1/auth/login"),
                new HttpEntity<>("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}", headers),
                String.class);
        assertEquals(200, respuesta.getStatusCode().value());
        return objectMapper.readTree(respuesta.getBody());
    }

    private JsonNode registrar(String prefijo) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String email = prefijo + "." + System.nanoTime() + "@pruebas.com";
        ResponseEntity<String> respuesta = rest.postForEntity(url("/api/v1/auth/register"),
                new HttpEntity<>("{\"nombre\":\"Concurrente\",\"email\":\"" + email
                        + "\",\"password\":\"Secreta123\"}", headers),
                String.class);
        assertEquals(201, respuesta.getStatusCode().value());
        return objectMapper.readTree(respuesta.getBody());
    }

    private HttpHeaders conToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void dosSolicitudesSimultaneasSobreElUltimoEjemplarSoloUnaTieneExito() throws Exception {
        JsonNode admin = login("admin@biblioteca.com", "Admin123*");
        String tokenAdmin = admin.get("token").asText();

        String isbn = "978-9-99-" + System.nanoTime() + "-1";
        ResponseEntity<String> libroCreado = rest.postForEntity(url("/api/v1/libros"),
                new HttpEntity<>("{\"isbn\":\"" + isbn + "\",\"titulo\":\"Ultimo Ejemplar\","
                        + "\"autor\":\"Autor\",\"categoria\":\"Concurrencia\",\"stockTotal\":1,"
                        + "\"stockDisponible\":1}", conToken(tokenAdmin)),
                String.class);
        assertEquals(201, libroCreado.getStatusCode().value());
        long libroId = objectMapper.readTree(libroCreado.getBody()).get("id").asLong();

        JsonNode lector1 = registrar("conc.uno");
        JsonNode lector2 = registrar("conc.dos");
        long usuario1 = lector1.get("id").asLong();
        long usuario2 = lector2.get("id").asLong();

        CountDownLatch listos = new CountDownLatch(1);
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        List<Future<Integer>> futuros = new ArrayList<>();

        for (long usuarioId : new long[]{usuario1, usuario2}) {
            futuros.add(ejecutor.submit(() -> {
                HttpHeaders headers = conToken(tokenAdmin);
                HttpEntity<String> entidad = new HttpEntity<>(
                        "{\"usuarioId\":" + usuarioId + ",\"libroId\":" + libroId + "}", headers);
                listos.await();
                ResponseEntity<String> respuesta = rest.postForEntity(url("/api/v1/prestamos"), entidad, String.class);
                return respuesta.getStatusCode().value();
            }));
        }

        listos.countDown();
        int exitos = 0;
        List<Integer> estados = new ArrayList<>();
        for (Future<Integer> futuro : futuros) {
            int estado = futuro.get(30, TimeUnit.SECONDS);
            estados.add(estado);
            if (estado >= 200 && estado < 300) {
                exitos++;
            }
        }
        ejecutor.shutdownNow();

        assertEquals(1, exitos, "solo una de las dos solicitudes simultaneas debe obtener el ejemplar: " + estados);

        // el stock nunca queda negativo
        ResponseEntity<String> consulta = rest.exchange(url("/api/v1/libros/" + libroId),
                HttpMethod.GET, new HttpEntity<>(conToken(tokenAdmin)), String.class);
        int stock = objectMapper.readTree(consulta.getBody()).get("stockDisponible").asInt();
        assertEquals(0, stock, "el stock debe quedar en 0, nunca negativo");

        // la aplicacion sigue respondiendo
        ResponseEntity<String> listado = rest.exchange(url("/api/v1/libros"),
                HttpMethod.GET, new HttpEntity<>(conToken(tokenAdmin)), String.class);
        assertEquals(200, listado.getStatusCode().value());
        assertNotNull(listado.getBody());
        assertTrue(listado.getBody().length() > 0);
    }
}
