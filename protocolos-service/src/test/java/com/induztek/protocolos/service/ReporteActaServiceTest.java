package com.induztek.protocolos.service;

import com.induztek.protocolos.client.EquiposCliente;
import com.induztek.protocolos.dto.InstalacionExterno;
import com.induztek.protocolos.dto.ReporteActaRequest;
import com.induztek.protocolos.dto.ReporteActaResponse;
import com.induztek.protocolos.dto.ResponderReporteRequest;
import com.induztek.protocolos.dto.UsuarioExterno;
import com.induztek.protocolos.model.EstadoProtocolo;
import com.induztek.protocolos.model.EstadoReporte;
import com.induztek.protocolos.model.HistorialEstado;
import com.induztek.protocolos.model.Protocolo;
import com.induztek.protocolos.model.ReporteActa;
import com.induztek.protocolos.model.TipoVisita;
import com.induztek.protocolos.model.Visita;
import com.induztek.protocolos.repository.HistorialEstadoRepository;
import com.induztek.protocolos.repository.ProtocoloRepository;
import com.induztek.protocolos.repository.ReporteActaRepository;
import com.induztek.protocolos.repository.VisitaRepository;
import com.induztek.protocolos.util.Roles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReporteActaServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 12, 0);

    @Mock
    private ReporteActaRepository reporteRepository;
    @Mock
    private VisitaRepository visitaRepository;
    @Mock
    private ProtocoloRepository protocoloRepository;
    @Mock
    private HistorialEstadoRepository historialRepository;
    @Mock
    private ActorService actorService;
    @Mock
    private EquiposCliente equiposCliente;

    private ReporteActaService servicio;
    private Visita visita;

    private static UsuarioExterno usuario(Long id, String rol) {
        return new UsuarioExterno(id, 1L, rol, rol, "1-9", "Nombre", "a@b.cl", null, true, null, null, null, false);
    }

    private static InstalacionExterno instalacion(Long clienteId) {
        return new InstalacionExterno(2L, clienteId, "Planta", null, 1L, "Lampa", 1L, "RM", null);
    }

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);
        servicio = new ReporteActaService(reporteRepository, visitaRepository, protocoloRepository,
                historialRepository, actorService, equiposCliente, reloj);
        visita = Visita.builder().id(4L).numeroOt("OT-1").instalacionId(2L).fecha(LocalDate.of(2026, 11, 10))
                .tipoVisita(TipoVisita.SOLO_PRUEBAS).fechaHoraCierre(AHORA).build();
        when(actorService.exigirUsuarioActivo(30L)).thenReturn(usuario(30L, Roles.CLIENTE));
        when(actorService.exigirUsuarioActivo(31L)).thenReturn(usuario(31L, Roles.CLIENTE));
        when(actorService.exigirUsuarioActivo(9L)).thenReturn(usuario(9L, Roles.INGENIERO));
        when(actorService.exigirRol(any(), any(), any(String[].class))).thenReturn(usuario(9L, Roles.INGENIERO));
        when(visitaRepository.findById(4L)).thenReturn(Optional.of(visita));
        when(equiposCliente.buscarInstalacion(2L)).thenReturn(Optional.of(instalacion(30L)));
        when(reporteRepository.save(any(ReporteActa.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Protocolo protocolo(EstadoProtocolo estado) {
        return Protocolo.builder().id(7L).numero("PT-2026-0007").visita(visita).equipoId(5L)
                .usuarioRegistraId(8L).fechaHoraPrueba(AHORA).estado(estado).build();
    }

    private ReporteActa reporte(Long id, Long clienteId, EstadoReporte estado) {
        return ReporteActa.builder().id(id).visita(visita).clienteId(clienteId).texto("t").estado(estado)
                .fechaHoraCreacion(AHORA).fechaHoraActualizacion(AHORA).build();
    }

    private void assertEstado(HttpStatus esperado, Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(esperado.value(), error.getStatusCode().value());
    }

    @Test
    void crear_sinProtocolo_guardaReporteAbierto() {
        ReporteActaResponse r = servicio.crear(30L, 4L, new ReporteActaRequest(null, "  Falta una firma  "));

        assertEquals(EstadoReporte.ABIERTO, r.estado());
        assertEquals("Falta una firma", r.texto());
        assertEquals("OT-1", r.numeroOt());
        assertNull(r.protocoloId());
    }

    @Test
    void crear_sobreProtocoloFormalizado_loPasaAObservadoYRegistraElHistorial() {
        Protocolo p = protocolo(EstadoProtocolo.FORMALIZADO);
        when(protocoloRepository.findById(7L)).thenReturn(Optional.of(p));

        ReporteActaResponse r = servicio.crear(30L, 4L, new ReporteActaRequest(7L, "Valor dudoso"));

        assertEquals(EstadoProtocolo.OBSERVADO, p.getEstado());
        assertEquals("PT-2026-0007", r.protocoloNumero());
        ArgumentCaptor<HistorialEstado> historial = ArgumentCaptor.forClass(HistorialEstado.class);
        verify(historialRepository).save(historial.capture());
        assertEquals(EstadoProtocolo.FORMALIZADO, historial.getValue().getEstadoAnterior());
        assertEquals(EstadoProtocolo.OBSERVADO, historial.getValue().getEstadoNuevo());
        assertEquals(30L, historial.getValue().getUsuarioId());
        assertEquals("Reporte del cliente: Valor dudoso", historial.getValue().getMotivo());
    }

    @Test
    void crear_sobreProtocoloNoFormalizado_noCambiaSuEstado() {
        Protocolo p = protocolo(EstadoProtocolo.REGISTRADO_TERRENO);
        when(protocoloRepository.findById(7L)).thenReturn(Optional.of(p));

        servicio.crear(30L, 4L, new ReporteActaRequest(7L, "Consulta"));

        assertEquals(EstadoProtocolo.REGISTRADO_TERRENO, p.getEstado());
        verify(historialRepository, never()).save(any());
    }

    @Test
    void crear_sobreProtocoloAnulado_lanza409() {
        when(protocoloRepository.findById(7L)).thenReturn(Optional.of(protocolo(EstadoProtocolo.ANULADO)));

        assertEstado(HttpStatus.CONFLICT, () -> servicio.crear(30L, 4L, new ReporteActaRequest(7L, "x")));
    }

    @Test
    void crear_conProtocoloDeOtraActa_lanza400() {
        Visita otra = Visita.builder().id(99L).build();
        Protocolo p = protocolo(EstadoProtocolo.FORMALIZADO);
        p.setVisita(otra);
        when(protocoloRepository.findById(7L)).thenReturn(Optional.of(p));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(30L, 4L, new ReporteActaRequest(7L, "x")));
    }

    @Test
    void crear_conProtocoloInexistente_lanza400() {
        when(protocoloRepository.findById(7L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(30L, 4L, new ReporteActaRequest(7L, "x")));
    }

    @Test
    void crear_siNoEsCliente_lanza403() {
        assertEstado(HttpStatus.FORBIDDEN, () -> servicio.crear(9L, 4L, new ReporteActaRequest(null, "x")));
    }

    @Test
    void crear_conActaInexistente_lanza404() {
        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.crear(30L, 77L, new ReporteActaRequest(null, "x")));
    }

    @Test
    void crear_deLaActaDeOtroCliente_lanza404() {
        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.crear(31L, 4L, new ReporteActaRequest(null, "x")));
    }

    @Test
    void crear_siLaInstalacionYaNoExiste_lanza404() {
        when(equiposCliente.buscarInstalacion(2L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.crear(30L, 4L, new ReporteActaRequest(null, "x")));
    }

    @Test
    void crear_conActaSinCerrar_lanza409() {
        visita.setFechaHoraCierre(null);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.crear(30L, 4L, new ReporteActaRequest(null, "x")));
    }

    @Test
    void crear_sinTexto_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(30L, 4L, new ReporteActaRequest(null, "  ")));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(30L, 4L, new ReporteActaRequest(null, null)));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(30L, 4L, null));
    }

    @Test
    void listar_comoCliente_soloVeLosSuyos() {
        when(reporteRepository.findByClienteIdOrderByIdDesc(30L))
                .thenReturn(List.of(reporte(1L, 30L, EstadoReporte.ABIERTO), reporte(2L, 30L, EstadoReporte.RESUELTO)));

        List<ReporteActaResponse> lista = servicio.listar(30L, null, EstadoReporte.ABIERTO);

        assertEquals(1, lista.size());
        assertEquals(1L, lista.get(0).id());
    }

    @Test
    void listar_comoPersonal_veTodosYFiltraPorActa() {
        Visita otra = Visita.builder().id(99L).numeroOt("OT-9").build();
        ReporteActa deOtra = reporte(3L, 31L, EstadoReporte.ABIERTO);
        deOtra.setVisita(otra);
        when(reporteRepository.findAllByOrderByIdDesc())
                .thenReturn(List.of(deOtra, reporte(1L, 30L, EstadoReporte.ABIERTO)));

        assertEquals(2, servicio.listar(9L, null, null).size());
        assertEquals(1, servicio.listar(9L, 4L, null).size());
    }

    @Test
    void obtener_delPropioCliente_devuelveElReporte() {
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(reporte(1L, 30L, EstadoReporte.ABIERTO)));

        assertEquals(1L, servicio.obtener(30L, 1L).id());
        assertEquals(1L, servicio.obtener(9L, 1L).id());
    }

    @Test
    void obtener_deOtroCliente_lanza404() {
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(reporte(1L, 30L, EstadoReporte.ABIERTO)));

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.obtener(31L, 1L));
    }

    @Test
    void obtener_inexistente_lanza404() {
        when(reporteRepository.findById(1L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.obtener(9L, 1L));
    }

    @Test
    void responder_enRevision_noExigeRespuesta() {
        ReporteActa r = reporte(1L, 30L, EstadoReporte.ABIERTO);
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(r));

        ReporteActaResponse resultado = servicio.responder(9L, 1L, new ResponderReporteRequest(EstadoReporte.EN_REVISION, null));

        assertEquals(EstadoReporte.EN_REVISION, resultado.estado());
        assertEquals(9L, resultado.respondidoPorId());
        assertNull(resultado.respuesta());
    }

    @Test
    void responder_resuelto_guardaLaRespuesta() {
        ReporteActa r = reporte(1L, 30L, EstadoReporte.EN_REVISION);
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(r));

        ReporteActaResponse resultado = servicio.responder(9L, 1L,
                new ResponderReporteRequest(EstadoReporte.RESUELTO, " Corregido y reemitido "));

        assertEquals(EstadoReporte.RESUELTO, resultado.estado());
        assertEquals("Corregido y reemitido", resultado.respuesta());
    }

    @Test
    void responder_cerrarSinRespuesta_lanza400() {
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(reporte(1L, 30L, EstadoReporte.ABIERTO)));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.responder(9L, 1L,
                new ResponderReporteRequest(EstadoReporte.RECHAZADO, " ")));
    }

    @Test
    void responder_conEstadoInvalido_lanza400() {
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(reporte(1L, 30L, EstadoReporte.ABIERTO)));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.responder(9L, 1L,
                new ResponderReporteRequest(EstadoReporte.ABIERTO, "x")));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.responder(9L, 1L, new ResponderReporteRequest(null, "x")));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.responder(9L, 1L, null));
    }

    @Test
    void responder_reporteYaCerrado_lanza409() {
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(reporte(1L, 30L, EstadoReporte.RESUELTO)));
        when(reporteRepository.findById(2L)).thenReturn(Optional.of(reporte(2L, 30L, EstadoReporte.RECHAZADO)));

        assertEstado(HttpStatus.CONFLICT, () -> servicio.responder(9L, 1L,
                new ResponderReporteRequest(EstadoReporte.EN_REVISION, null)));
        assertEstado(HttpStatus.CONFLICT, () -> servicio.responder(9L, 2L,
                new ResponderReporteRequest(EstadoReporte.EN_REVISION, null)));
    }

    @Test
    void responder_reporteInexistente_lanza404() {
        when(reporteRepository.findById(1L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.responder(9L, 1L,
                new ResponderReporteRequest(EstadoReporte.EN_REVISION, null)));
    }
}
