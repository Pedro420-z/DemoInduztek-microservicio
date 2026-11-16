package com.induztek.protocolos.service;

import com.induztek.protocolos.client.UsuariosCliente;
import com.induztek.protocolos.dto.RecepcionRequest;
import com.induztek.protocolos.dto.UsuarioExterno;
import com.induztek.protocolos.dto.VisitaRequest;
import com.induztek.protocolos.dto.VisitaResponse;
import com.induztek.protocolos.model.RecepcionActa;
import com.induztek.protocolos.model.TipoVisita;
import com.induztek.protocolos.model.Visita;
import com.induztek.protocolos.repository.RecepcionActaRepository;
import com.induztek.protocolos.repository.VisitaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitaServiceTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 16);

    @Mock
    private VisitaRepository visitaRepository;

    @Mock
    private RecepcionActaRepository recepcionActaRepository;

    @Mock
    private ActorService actorService;

    @Mock
    private UsuariosCliente usuariosCliente;

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);

    private VisitaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new VisitaService(visitaRepository, recepcionActaRepository, actorService, usuariosCliente, reloj);
    }

    private UsuarioExterno usuario(Long id, String rol, boolean activo) {
        return new UsuarioExterno(id, 1L, rol, rol, "11111111-1", "Usuario " + id,
                "u" + id + "@induztek.cl", null, activo, null, null, null, false);
    }

    private void usuarioExiste(Long id, String rol, boolean activo) {
        when(usuariosCliente.buscar(id)).thenReturn(Optional.of(usuario(id, rol, activo)));
    }

    private VisitaRequest peticion(String ot, Long instalacionId, LocalDate fecha, TipoVisita tipo,
                                   Long ingenieroId, List<Long> participantes) {
        return new VisitaRequest(ot, instalacionId, fecha, tipo, ingenieroId, participantes);
    }

    private VisitaRequest peticionValida() {
        return peticion(" ot-2026-1187 ", 1L, FECHA, TipoVisita.SOLO_PRUEBAS, 10L, List.of(21L, 20L, 20L));
    }

    private Visita visita(Long id, String ot, Long instalacionId, Long ingenieroId, LocalDateTime cierre) {
        return Visita.builder().id(id).numeroOt(ot).instalacionId(instalacionId).fecha(FECHA)
                .tipoVisita(TipoVisita.PRUEBAS_Y_MANT).ingenieroId(ingenieroId).fechaHoraCierre(cierre)
                .participantesIds(new LinkedHashSet<>(Set.of(20L))).build();
    }

    private void assertEstado(HttpStatus esperado, Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(esperado.value(), error.getStatusCode().value());
    }

    private void listaDeDosVisitas() {
        when(visitaRepository.findAllByOrderByFechaDescIdDesc()).thenReturn(List.of(
                visita(1L, "OT-1", 1L, 10L, null),
                visita(2L, "OT-2", 2L, 11L, LocalDateTime.of(2026, 11, 16, 17, 0))));
    }

    @Test
    void crear_conDatosValidos_guardaLaVisitaConOtEnMayusculasYParticipantesSinRepetir() {
        usuarioExiste(10L, "INGENIERO", true);
        usuarioExiste(20L, "TECNICO", true);
        usuarioExiste(21L, "TECNICO", true);
        when(visitaRepository.save(any(Visita.class))).thenAnswer(i -> i.getArgument(0));

        VisitaResponse respuesta = servicio.crear(1L, peticionValida());

        verify(actorService).exigirPersonalActivo(1L);
        assertEquals("OT-2026-1187", respuesta.numeroOt());
        assertEquals(List.of(20L, 21L), respuesta.participantesIds());
        assertFalse(respuesta.cerrada());
        assertNull(respuesta.fechaHoraCierre());
        assertNull(respuesta.recepcion());
    }

    @Test
    void crear_sinPermiso_noGuardaNada() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(actorService).exigirPersonalActivo(4L);

        assertEstado(HttpStatus.FORBIDDEN, () -> servicio.crear(4L, peticionValida()));
        verify(visitaRepository, never()).save(any(Visita.class));
    }

    @Test
    void crear_sinNumeroDeOt_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L,
                peticion(" ", 1L, FECHA, TipoVisita.SOLO_PRUEBAS, 10L, List.of(20L))));
    }

    @Test
    void crear_conOtNulo_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L,
                peticion(null, 1L, FECHA, TipoVisita.SOLO_PRUEBAS, 10L, List.of(20L))));
    }

    @Test
    void crear_sinInstalacion_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L,
                peticion("OT-1", null, FECHA, TipoVisita.SOLO_PRUEBAS, 10L, List.of(20L))));
    }

    @Test
    void crear_sinFecha_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L,
                peticion("OT-1", 1L, null, TipoVisita.SOLO_PRUEBAS, 10L, List.of(20L))));
    }

    @Test
    void crear_sinTipoDeVisita_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L,
                peticion("OT-1", 1L, FECHA, null, 10L, List.of(20L))));
    }

    @Test
    void crear_sinIngenieroACargo_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L,
                peticion("OT-1", 1L, FECHA, TipoVisita.SOLO_PRUEBAS, null, List.of(20L))));
    }

    @Test
    void crear_conIngenieroInexistente_lanza400() {
        when(usuariosCliente.buscar(10L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L, peticionValida()));
    }

    @Test
    void crear_conResponsableQueNoEsIngeniero_lanza400() {
        usuarioExiste(10L, "TECNICO", true);

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L, peticionValida()));
    }

    @Test
    void crear_conIngenieroInactivo_lanza400() {
        usuarioExiste(10L, "INGENIERO", false);

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L, peticionValida()));
    }

    @Test
    void crear_sinParticipantes_lanza400() {
        usuarioExiste(10L, "INGENIERO", true);

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L,
                peticion("OT-1", 1L, FECHA, TipoVisita.SOLO_PRUEBAS, 10L, null)));
    }

    @Test
    void crear_conListaDeParticipantesVacia_lanza400() {
        usuarioExiste(10L, "INGENIERO", true);

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L,
                peticion("OT-1", 1L, FECHA, TipoVisita.SOLO_PRUEBAS, 10L, List.of())));
    }

    @Test
    void crear_conParticipanteInexistente_lanza400() {
        usuarioExiste(10L, "INGENIERO", true);
        when(usuariosCliente.buscar(20L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L,
                peticion("OT-1", 1L, FECHA, TipoVisita.SOLO_PRUEBAS, 10L, List.of(20L))));
    }

    @Test
    void crear_conParticipanteQueNoEsTecnico_lanza400() {
        usuarioExiste(10L, "INGENIERO", true);
        usuarioExiste(20L, "JEFE_TECNICO", true);

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L,
                peticion("OT-1", 1L, FECHA, TipoVisita.SOLO_PRUEBAS, 10L, List.of(20L))));
    }

    @Test
    void crear_conTecnicoInactivo_lanza400() {
        usuarioExiste(10L, "INGENIERO", true);
        usuarioExiste(20L, "TECNICO", false);

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L,
                peticion("OT-1", 1L, FECHA, TipoVisita.SOLO_PRUEBAS, 10L, List.of(20L))));
    }

    @Test
    void crear_conOtRepetida_lanza409() {
        usuarioExiste(10L, "INGENIERO", true);
        usuarioExiste(20L, "TECNICO", true);
        when(visitaRepository.existsByNumeroOt("OT-1")).thenReturn(true);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.crear(1L,
                peticion("ot-1", 1L, FECHA, TipoVisita.SOLO_PRUEBAS, 10L, List.of(20L))));
        verify(visitaRepository, never()).save(any(Visita.class));
    }

    @Test
    void listar_sinFiltros_devuelveTodasLasVisitas() {
        listaDeDosVisitas();

        assertEquals(2, servicio.listar(null, null, null, null).size());
        assertEquals(2, servicio.listar("  ", null, null, null).size());
    }

    @Test
    void listar_buscaPorNumeroDeOt() {
        when(visitaRepository.findByNumeroOtContainingIgnoreCaseOrderByFechaDescIdDesc("ot-2"))
                .thenReturn(List.of(visita(2L, "OT-2", 2L, 11L, null)));

        List<VisitaResponse> respuesta = servicio.listar(" ot-2 ", null, null, null);

        assertEquals(1, respuesta.size());
        assertEquals("OT-2", respuesta.get(0).numeroOt());
    }

    @Test
    void listar_filtraPorInstalacion() {
        listaDeDosVisitas();

        List<VisitaResponse> respuesta = servicio.listar(null, 2L, null, null);

        assertEquals(1, respuesta.size());
        assertEquals("OT-2", respuesta.get(0).numeroOt());
    }

    @Test
    void listar_filtraPorIngenieroACargo() {
        listaDeDosVisitas();

        List<VisitaResponse> respuesta = servicio.listar(null, null, 10L, null);

        assertEquals(1, respuesta.size());
        assertEquals("OT-1", respuesta.get(0).numeroOt());
    }

    @Test
    void listar_filtraPorVisitasCerradas() {
        listaDeDosVisitas();

        List<VisitaResponse> respuesta = servicio.listar(null, null, null, true);

        assertEquals(1, respuesta.size());
        assertTrue(respuesta.get(0).cerrada());
    }

    @Test
    void listar_filtraPorVisitasAbiertas() {
        listaDeDosVisitas();

        List<VisitaResponse> respuesta = servicio.listar(null, null, null, false);

        assertEquals(1, respuesta.size());
        assertFalse(respuesta.get(0).cerrada());
    }

    @Test
    void obtener_conRecepcionRegistrada_laIncluyeEnLaRespuesta() {
        when(visitaRepository.findById(2L)).thenReturn(Optional.of(visita(2L, "OT-2", 2L, 11L, LocalDateTime.of(2026, 11, 16, 17, 0))));
        when(recepcionActaRepository.findById(2L)).thenReturn(Optional.of(RecepcionActa.builder()
                .visitaId(2L).receptorNombre("Marcela Fuentes").receptorRut("14226791-8")
                .receptorCargo("Jefa de Mantenimiento").fechaHoraRecepcion(LocalDateTime.of(2026, 11, 16, 17, 30)).build()));

        VisitaResponse respuesta = servicio.obtener(2L);

        assertNotNull(respuesta.recepcion());
        assertEquals("Marcela Fuentes", respuesta.recepcion().receptorNombre());
    }

    @Test
    void obtener_conIdInexistente_lanza404() {
        when(visitaRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.obtener(99L));
    }

    @Test
    void registrarRecepcion_enVisitaCerrada_guardaLosDatosDeQuienRecibe() {
        when(visitaRepository.findById(2L)).thenReturn(Optional.of(visita(2L, "OT-2", 2L, 11L, LocalDateTime.of(2026, 11, 16, 17, 0))));

        servicio.registrarRecepcion(2L, new RecepcionRequest(" Marcela Fuentes ", "14.226.791-8", " Jefa de Mantenimiento "));

        ArgumentCaptor<RecepcionActa> guardada = ArgumentCaptor.forClass(RecepcionActa.class);
        verify(recepcionActaRepository).save(guardada.capture());
        assertEquals(2L, guardada.getValue().getVisitaId());
        assertEquals("Marcela Fuentes", guardada.getValue().getReceptorNombre());
        assertEquals("14226791-8", guardada.getValue().getReceptorRut());
        assertEquals("Jefa de Mantenimiento", guardada.getValue().getReceptorCargo());
        assertEquals(LocalDateTime.of(2026, 11, 15, 12, 0), guardada.getValue().getFechaHoraRecepcion());
    }

    @Test
    void registrarRecepcion_conVisitaInexistente_lanza404() {
        when(visitaRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.registrarRecepcion(99L,
                new RecepcionRequest("Marcela", "14.226.791-8", "Jefa")));
    }

    @Test
    void registrarRecepcion_enVisitaAbierta_lanza409() {
        when(visitaRepository.findById(1L)).thenReturn(Optional.of(visita(1L, "OT-1", 1L, 10L, null)));

        assertEstado(HttpStatus.CONFLICT, () -> servicio.registrarRecepcion(1L,
                new RecepcionRequest("Marcela", "14.226.791-8", "Jefa")));
        verify(recepcionActaRepository, never()).save(any(RecepcionActa.class));
    }

    @Test
    void registrarRecepcion_siYaFueRecibida_lanza409() {
        when(visitaRepository.findById(2L)).thenReturn(Optional.of(visita(2L, "OT-2", 2L, 11L, LocalDateTime.of(2026, 11, 16, 17, 0))));
        when(recepcionActaRepository.existsById(2L)).thenReturn(true);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.registrarRecepcion(2L,
                new RecepcionRequest("Marcela", "14.226.791-8", "Jefa")));
    }

    @Test
    void registrarRecepcion_sinNombre_lanza400() {
        when(visitaRepository.findById(2L)).thenReturn(Optional.of(visita(2L, "OT-2", 2L, 11L, LocalDateTime.of(2026, 11, 16, 17, 0))));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrarRecepcion(2L,
                new RecepcionRequest(" ", "14.226.791-8", "Jefa")));
    }

    @Test
    void registrarRecepcion_sinCargo_lanza400() {
        when(visitaRepository.findById(2L)).thenReturn(Optional.of(visita(2L, "OT-2", 2L, 11L, LocalDateTime.of(2026, 11, 16, 17, 0))));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrarRecepcion(2L,
                new RecepcionRequest("Marcela", "14.226.791-8", null)));
    }

    @Test
    void registrarRecepcion_conRutInvalido_lanza400() {
        when(visitaRepository.findById(2L)).thenReturn(Optional.of(visita(2L, "OT-2", 2L, 11L, LocalDateTime.of(2026, 11, 16, 17, 0))));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrarRecepcion(2L,
                new RecepcionRequest("Marcela", "14.226.791-9", "Jefa")));
        verify(recepcionActaRepository, never()).save(any(RecepcionActa.class));
    }

    @Test
    void registrarRecepcion_conNombreNulo_lanza400() {
        when(visitaRepository.findById(2L)).thenReturn(Optional.of(visita(2L, "OT-2", 2L, 11L, LocalDateTime.of(2026, 11, 16, 17, 0))));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrarRecepcion(2L,
                new RecepcionRequest(null, "14.226.791-8", "Jefa")));
    }
}
