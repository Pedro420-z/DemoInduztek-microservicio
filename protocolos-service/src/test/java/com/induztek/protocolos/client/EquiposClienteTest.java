package com.induztek.protocolos.client;

import com.induztek.protocolos.dto.EquipoExterno;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquiposClienteTest {

    private static final String EQUIPO = "{\"id\":5,\"instalacionId\":2,\"instalacionNombre\":\"Planta Norte\","
            + "\"clienteId\":30,\"tag\":\"TR-01\",\"tipoEquipoId\":1,\"tipoEquipoNombre\":\"Transformador\","
            + "\"clasificacion\":\"POTENCIA\",\"marca\":null,\"modelo\":null,\"numeroSerie\":null,"
            + "\"anioFabricacion\":2015,\"potenciaKva\":1000,\"tensionPrimariaKv\":13.2,\"tensionSecundariaKv\":0.4,"
            + "\"corrienteNominalA\":null,\"maxContactosUohm\":150.5,\"observaciones\":null}";

    private HttpServer servidor;

    @BeforeEach
    void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/equipos/5", exchange -> responder(exchange, 200, EQUIPO));
        servidor.createContext("/api/equipos/404", exchange -> responder(exchange, 404, "{}"));
        servidor.createContext("/api/equipos/500", exchange -> responder(exchange, 500, "{}"));
        servidor.start();
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    private String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    private static void responder(HttpExchange exchange, int codigo, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
    }

    @Test
    void buscar_conEquipoExistente_devuelveSusDatos() {
        Optional<EquipoExterno> equipo = new EquiposCliente(url()).buscar(5L);

        assertTrue(equipo.isPresent());
        assertEquals("TR-01", equipo.get().tag());
        assertEquals(1L, equipo.get().tipoEquipoId());
        assertEquals(2L, equipo.get().instalacionId());
    }

    @Test
    void buscar_conEquipoInexistente_devuelveVacio() {
        assertFalse(new EquiposCliente(url()).buscar(404L).isPresent());
    }

    @Test
    void buscar_siElServicioFalla_lanza503() {
        EquiposCliente cliente = new EquiposCliente(url());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> cliente.buscar(500L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), error.getStatusCode().value());
    }

    @Test
    void buscar_siElServicioNoResponde_lanza503() throws IOException {
        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoLibre = socket.getLocalPort();
        }
        EquiposCliente cliente = new EquiposCliente("http://127.0.0.1:" + puertoLibre);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> cliente.buscar(5L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), error.getStatusCode().value());
    }
}
