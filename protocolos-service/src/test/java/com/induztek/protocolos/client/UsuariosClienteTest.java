package com.induztek.protocolos.client;

import com.induztek.protocolos.dto.UsuarioExterno;
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

class UsuariosClienteTest {

    private static final String USUARIO = "{\"id\":7,\"rolId\":2,\"rolCodigo\":\"INGENIERO\",\"rolNombre\":\"Ingeniero\","
            + "\"rut\":\"11111111-1\",\"nombre\":\"Ana Torres\",\"correo\":\"ana@induztek.cl\",\"telefono\":null,"
            + "\"activo\":true,\"giro\":null,\"contactoNombre\":null,\"contactoCargo\":null,"
            + "\"debeCambiarContrasena\":false}";

    private HttpServer servidor;

    @BeforeEach
    void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/usuarios/7", exchange -> responder(exchange, 200, USUARIO));
        servidor.createContext("/api/usuarios/404", exchange -> responder(exchange, 404, "{}"));
        servidor.createContext("/api/usuarios/500", exchange -> responder(exchange, 500, "{}"));
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
    void buscar_conUsuarioExistente_devuelveSusDatos() {
        Optional<UsuarioExterno> usuario = new UsuariosCliente(url()).buscar(7L);

        assertTrue(usuario.isPresent());
        assertEquals("INGENIERO", usuario.get().rolCodigo());
        assertEquals("Ana Torres", usuario.get().nombre());
        assertTrue(usuario.get().activo());
    }

    @Test
    void buscar_conUsuarioInexistente_devuelveVacio() {
        assertFalse(new UsuariosCliente(url()).buscar(404L).isPresent());
    }

    @Test
    void buscar_siElServicioFalla_lanza503() {
        UsuariosCliente cliente = new UsuariosCliente(url());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> cliente.buscar(500L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), error.getStatusCode().value());
    }

    @Test
    void buscar_siElServicioNoResponde_lanza503() throws IOException {
        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoLibre = socket.getLocalPort();
        }
        UsuariosCliente cliente = new UsuariosCliente("http://127.0.0.1:" + puertoLibre);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> cliente.buscar(7L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), error.getStatusCode().value());
    }
}
