package com.induztek.reportes.client;

import com.induztek.reportes.dto.EquipoExterno;
import com.induztek.reportes.dto.InstalacionExterno;
import com.induztek.reportes.dto.ProtocoloExterno;
import com.induztek.reportes.dto.UsuarioExterno;
import com.induztek.reportes.dto.VisitaExterno;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientesTest {

    private static final String USUARIO = "{\"id\":7,\"rolId\":2,\"rolCodigo\":\"CLIENTE\",\"rolNombre\":\"Cliente\","
            + "\"rut\":\"11111111-1\",\"nombre\":\"Ana\",\"correo\":\"a@b.cl\",\"telefono\":null,\"activo\":true,"
            + "\"giro\":null,\"contactoNombre\":null,\"contactoCargo\":null,\"debeCambiarContrasena\":false}";
    private static final String EQUIPO = "{\"id\":5,\"instalacionId\":2,\"instalacionNombre\":\"Planta\",\"clienteId\":30,"
            + "\"tag\":\"TR-01\",\"tipoEquipoId\":1,\"tipoEquipoNombre\":\"Transformador\",\"clasificacion\":\"POTENCIA\","
            + "\"marca\":null,\"modelo\":null,\"numeroSerie\":null,\"anioFabricacion\":2015,\"potenciaKva\":1000,"
            + "\"tensionPrimariaKv\":13.2,\"tensionSecundariaKv\":0.4,\"corrienteNominalA\":null,\"maxContactosUohm\":150.5,"
            + "\"observaciones\":null}";
    private static final String INSTALACION = "{\"id\":2,\"clienteId\":30,\"nombre\":\"Planta Norte\",\"ubicacion\":\"Ruta 5\","
            + "\"comunaId\":1,\"comunaNombre\":\"Santiago\",\"regionId\":13,\"regionNombre\":\"Metropolitana\","
            + "\"observacionesAcceso\":null}";
    private static final String VISITA = "{\"id\":4,\"numeroOt\":\"OT-1\",\"instalacionId\":2,\"fecha\":\"2026-11-16\","
            + "\"tipoVisita\":\"SOLO_PRUEBAS\",\"ingenieroId\":10,\"participantesIds\":[20],"
            + "\"fechaHoraCierre\":\"2026-11-16T12:00:00\",\"cerrada\":true,\"recepcion\":{\"receptorNombre\":\"Luis\","
            + "\"receptorRut\":\"2-7\",\"receptorCargo\":\"Jefe\",\"fechaHoraRecepcion\":\"2026-11-16T13:00:00\"}}";
    private static final String PROTOCOLO = "{\"id\":1,\"numero\":\"PT-2026-0001\",\"visitaId\":4,\"visitaNumeroOt\":\"OT-1\","
            + "\"equipoId\":5,\"usuarioRegistraId\":8,\"fechaHoraPrueba\":\"2026-11-16T10:00:00\",\"temperaturaC\":20,"
            + "\"humedadPct\":50,\"descripcionMantenimiento\":null,\"observaciones\":null,\"estado\":\"FORMALIZADO\","
            + "\"protocoloReemplazadoId\":null,\"resultadoGlobal\":\"APROBADO\",\"atrasado\":false,"
            + "\"pruebas\":[{\"id\":1,\"tipoPruebaId\":1,\"tipoPruebaCodigo\":\"AISLACION\",\"tipoPruebaNombre\":\"Aislación\","
            + "\"elemento\":\"AT-BT\",\"momento\":\"UNICA\",\"realizada\":true,\"motivoNoRealizada\":null,"
            + "\"instrumentoId\":3,\"resultado\":\"APROBADO\",\"valorCalculado\":2.0,\"comentario\":null,"
            + "\"mediciones\":[{\"parametroCodigo\":\"LECTURA_1MIN\",\"parametroNombre\":\"Lectura\",\"unidad\":\"MΩ\","
            + "\"valor\":500}]}],\"historial\":[{\"id\":1,\"estadoAnterior\":null,\"estadoNuevo\":\"BORRADOR\",\"usuarioId\":8,"
            + "\"fechaHora\":\"2026-11-16T10:00:00\",\"motivo\":null}]}";

    private HttpServer servidor;

    @BeforeEach
    void iniciar() throws IOException {
        Map<String, String> rutas = Map.of(
                "/api/usuarios/7", USUARIO,
                "/api/equipos/5?clienteId=30", EQUIPO,
                "/api/equipos/5", EQUIPO,
                "/api/equipos?clienteId=30", "[" + EQUIPO + "]",
                "/api/equipos", "[" + EQUIPO + "]",
                "/api/instalaciones?clienteId=30", "[" + INSTALACION + "]",
                "/api/protocolos?equipoId=5&estado=FORMALIZADO", "[" + PROTOCOLO + "]",
                "/api/protocolos?atrasados=true", "[]",
                "/api/visitas?instalacionId=2&cerrada=true", "[" + VISITA + "]");
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", exchange -> {
            String uri = exchange.getRequestURI().toString();
            if (uri.startsWith("/api/protocolos?visitaId=9")) {
                responder(exchange, 500, "{}");
            } else if (uri.equals("/api/equipos/6") || uri.equals("/api/equipos?clienteId=31")) {
                responder(exchange, 200, "");
            } else if (rutas.containsKey(uri)) {
                responder(exchange, 200, rutas.get(uri));
            } else {
                responder(exchange, 404, "{}");
            }
        });
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
        exchange.sendResponseHeaders(codigo, bytes.length == 0 ? -1 : bytes.length);
        try (OutputStream salida = exchange.getResponseBody()) {
            salida.write(bytes);
        }
    }

    private void assertNoDisponible(Runnable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion::run);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), error.getStatusCode().value());
    }

    @Test
    void usuarios_buscaPorIdYDevuelveVacioSiNoExiste() {
        UsuariosCliente cliente = new UsuariosCliente(url());

        Optional<UsuarioExterno> usuario = cliente.buscar(7L);

        assertTrue(usuario.isPresent());
        assertEquals("CLIENTE", usuario.get().rolCodigo());
        assertFalse(cliente.buscar(99L).isPresent());
    }

    @Test
    void equipos_obtieneConYSinFiltroDeCliente() {
        EquiposCliente cliente = new EquiposCliente(url());

        Optional<EquipoExterno> conFiltro = cliente.obtener(5L, 30L);

        assertTrue(conFiltro.isPresent());
        assertEquals("TR-01", conFiltro.get().tag());
        assertTrue(cliente.obtener(5L, null).isPresent());
        assertFalse(cliente.obtener(5L, 31L).isPresent());
    }

    @Test
    void equipos_listaEquiposEInstalaciones() {
        EquiposCliente cliente = new EquiposCliente(url());

        List<EquipoExterno> equipos = cliente.listar(30L);
        List<InstalacionExterno> instalaciones = cliente.instalaciones(30L);

        assertEquals(1, equipos.size());
        assertEquals(1, cliente.listar(null).size());
        assertEquals("Planta Norte", instalaciones.get(0).nombre());
    }

    @Test
    void protocolos_listaProtocolosConSusPruebasEHistorial() {
        ProtocolosCliente cliente = new ProtocolosCliente(url());

        List<ProtocoloExterno> protocolos = cliente.protocolos(5L, null, "FORMALIZADO", null);

        assertEquals(1, protocolos.size());
        ProtocoloExterno protocolo = protocolos.get(0);
        assertEquals("PT-2026-0001", protocolo.numero());
        assertEquals("APROBADO", protocolo.resultadoGlobal());
        assertEquals("MΩ", protocolo.pruebas().get(0).mediciones().get(0).unidad());
        assertEquals("BORRADOR", protocolo.historial().get(0).estadoNuevo());
        assertTrue(cliente.protocolos(null, null, null, true).isEmpty());
    }

    @Test
    void protocolos_listaLasVisitasCerradasConSuRecepcion() {
        List<VisitaExterno> visitas = new ProtocolosCliente(url()).visitas(2L, true);

        assertEquals(1, visitas.size());
        assertEquals("OT-1", visitas.get(0).numeroOt());
        assertEquals("Luis", visitas.get(0).recepcion().receptorNombre());
    }

    @Test
    void consultas_siElServicioFalla_lanzan503() {
        ProtocolosCliente cliente = new ProtocolosCliente(url());

        assertNoDisponible(() -> cliente.protocolos(null, 9L, null, null));
        assertNoDisponible(() -> cliente.visitas(99L, true));
    }

    @Test
    void consultas_conCuerpoVacio_devuelvenVacio() {
        EquiposCliente cliente = new EquiposCliente(url());

        assertFalse(cliente.obtener(6L, null).isPresent());
        assertTrue(cliente.listar(31L).isEmpty());
    }

    @Test
    void consultas_siElServicioNoResponde_lanzan503() throws IOException {
        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoLibre = socket.getLocalPort();
        }
        String urlCaida = "http://127.0.0.1:" + puertoLibre;

        assertNoDisponible(() -> new UsuariosCliente(urlCaida).buscar(7L));
        assertNoDisponible(() -> new EquiposCliente(urlCaida).listar(null));
    }
}
