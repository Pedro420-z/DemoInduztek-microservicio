package com.induztek.protocolos.service;

import com.induztek.protocolos.client.EquiposCliente;
import com.induztek.protocolos.dto.EquipoExterno;
import com.induztek.protocolos.dto.MotivoRequest;
import com.induztek.protocolos.dto.ProtocoloActualizacionRequest;
import com.induztek.protocolos.dto.ProtocoloRequest;
import com.induztek.protocolos.dto.ProtocoloResponse;
import com.induztek.protocolos.dto.UsuarioExterno;
import com.induztek.protocolos.model.EstadoProtocolo;
import com.induztek.protocolos.model.HistorialEstado;
import com.induztek.protocolos.model.Protocolo;
import com.induztek.protocolos.model.Prueba;
import com.induztek.protocolos.model.TipoEquipoPrueba;
import com.induztek.protocolos.model.TipoPrueba;
import com.induztek.protocolos.model.TipoVisita;
import com.induztek.protocolos.model.Visita;
import com.induztek.protocolos.repository.HistorialEstadoRepository;
import com.induztek.protocolos.repository.ProtocoloRepository;
import com.induztek.protocolos.repository.PruebaRepository;
import com.induztek.protocolos.repository.TipoEquipoPruebaRepository;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProtocoloServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 12, 0);

    @Mock
    private ProtocoloRepository protocoloRepository;
    @Mock
    private PruebaRepository pruebaRepository;
    @Mock
    private HistorialEstadoRepository historialRepository;
    @Mock
    private VisitaRepository visitaRepository;
    @Mock
    private TipoEquipoPruebaRepository tipoEquipoPruebaRepository;
    @Mock
    private ActorService actorService;
    @Mock
    private EquiposCliente equiposCliente;
    @Mock
    private ProtocoloMapper mapper;

    private ProtocoloService servicio;
    private Visita visita;
    private ProtocoloResponse respuesta;

    private static UsuarioExterno usuario(Long id, String rol) {
        return new UsuarioExterno(id, 1L, rol, rol, "1-9", "Nombre", "a@b.cl", null, true, null, null, null, false);
    }

    private static EquipoExterno equipo(Long id, Long instalacionId, Long tipoId) {
        return new EquipoExterno(id, instalacionId, "Planta", 30L, "TR-01", tipoId, "Transformador", "POTENCIA",
                null, null, null, null, null, null, null, null, null, null);
    }

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);
        servicio = new ProtocoloService(protocoloRepository, pruebaRepository, historialRepository, visitaRepository,
                tipoEquipoPruebaRepository, actorService, equiposCliente, new ProtocoloPermisos(), mapper, reloj);
        visita = Visita.builder().id(4L).numeroOt("OT-1").instalacionId(2L).tipoVisita(TipoVisita.SOLO_PRUEBAS).build();
        respuesta = new ProtocoloResponse(1L, "PT-2026-0001", 4L, "OT-1", 5L, 8L, AHORA, null, null, null, null,
                EstadoProtocolo.BORRADOR, null, null, false, List.of(), List.of());
        when(actorService.exigirPersonalActivo(8L)).thenReturn(usuario(8L, Roles.TECNICO));
        when(actorService.exigirRol(any(), any(), any(String[].class))).thenReturn(usuario(9L, Roles.INGENIERO));
        when(visitaRepository.findById(4L)).thenReturn(Optional.of(visita));
        when(equiposCliente.buscar(5L)).thenReturn(Optional.of(equipo(5L, 2L, 1L)));
        when(protocoloRepository.existsByVisitaIdAndEquipoId(4L, 5L)).thenReturn(false);
        when(protocoloRepository.countByNumeroStartingWith("PT-2026-")).thenReturn(0L);
        when(protocoloRepository.save(any(Protocolo.class))).thenAnswer(i -> i.getArgument(0));
        when(mapper.aProtocolo(any(Protocolo.class))).thenReturn(respuesta);
    }

    private ProtocoloRequest solicitud(BigDecimal humedad, String mantenimiento, Long reemplazadoId) {
        return new ProtocoloRequest(4L, 5L, AHORA, new BigDecimal("20"), humedad, mantenimiento, "  obs  ", reemplazadoId);
    }

    private Protocolo existente(EstadoProtocolo estado, Long registraId) {
        return Protocolo.builder().id(1L).numero("PT-2026-0001").visita(visita).equipoId(5L)
                .usuarioRegistraId(registraId).fechaHoraPrueba(AHORA).temperaturaC(BigDecimal.TEN)
                .humedadPct(BigDecimal.TEN).estado(estado).build();
    }

    private void assertEstado(HttpStatus esperado, Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(esperado.value(), error.getStatusCode().value());
    }

    @Test
    void crear_conDatosValidos_guardaBorradorConNumeroEHistorial() {
        ProtocoloResponse resultado = servicio.crear(8L, solicitud(new BigDecimal("55"), null, null));

        ArgumentCaptor<Protocolo> captor = ArgumentCaptor.forClass(Protocolo.class);
        verify(protocoloRepository).save(captor.capture());
        assertSame(respuesta, resultado);
        assertEquals("PT-2026-0001", captor.getValue().getNumero());
        assertEquals(EstadoProtocolo.BORRADOR, captor.getValue().getEstado());
        assertEquals("obs", captor.getValue().getObservaciones());
        ArgumentCaptor<HistorialEstado> historial = ArgumentCaptor.forClass(HistorialEstado.class);
        verify(historialRepository).save(historial.capture());
        assertNull(historial.getValue().getEstadoAnterior());
        assertEquals(EstadoProtocolo.BORRADOR, historial.getValue().getEstadoNuevo());
    }

    @Test
    void crear_sinFechaUsaLaHoraActual() {
        ProtocoloRequest datos = new ProtocoloRequest(4L, 5L, null, null, null, null, null, null);

        servicio.crear(8L, datos);

        ArgumentCaptor<Protocolo> captor = ArgumentCaptor.forClass(Protocolo.class);
        verify(protocoloRepository).save(captor.capture());
        assertEquals(AHORA, captor.getValue().getFechaHoraPrueba());
    }

    @Test
    void crear_sinVisitaOSinEquipo_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(8L,
                new ProtocoloRequest(null, 5L, AHORA, null, null, null, null, null)));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(8L,
                new ProtocoloRequest(4L, null, AHORA, null, null, null, null, null)));
    }

    @Test
    void crear_conVisitaInexistente_lanza400() {
        when(visitaRepository.findById(4L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(8L, solicitud(null, null, null)));
    }

    @Test
    void crear_conVisitaCerrada_lanza409() {
        visita.setFechaHoraCierre(AHORA);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.crear(8L, solicitud(null, null, null)));
    }

    @Test
    void crear_conEquipoInexistente_lanza400() {
        when(equiposCliente.buscar(5L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(8L, solicitud(null, null, null)));
    }

    @Test
    void crear_conEquipoDeOtraInstalacion_lanza400() {
        when(equiposCliente.buscar(5L)).thenReturn(Optional.of(equipo(5L, 99L, 1L)));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(8L, solicitud(null, null, null)));
    }

    @Test
    void crear_siYaExisteProtocoloDelEquipoEnLaVisita_lanza409() {
        when(protocoloRepository.existsByVisitaIdAndEquipoId(4L, 5L)).thenReturn(true);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.crear(8L, solicitud(null, null, null)));
    }

    @Test
    void crear_conHumedadFueraDeRango_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(8L, solicitud(new BigDecimal("101"), null, null)));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(8L, solicitud(new BigDecimal("-1"), null, null)));
    }

    @Test
    void crear_conMantenimientoEnVisitaSoloDePruebas_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(8L, solicitud(null, "Limpieza", null)));
    }

    @Test
    void crear_conMantenimientoEnVisitaConMantenimiento_loGuarda() {
        visita.setTipoVisita(TipoVisita.PRUEBAS_Y_MANT);

        servicio.crear(8L, solicitud(null, " Limpieza ", null));

        ArgumentCaptor<Protocolo> captor = ArgumentCaptor.forClass(Protocolo.class);
        verify(protocoloRepository).save(captor.capture());
        assertEquals("Limpieza", captor.getValue().getDescripcionMantenimiento());
    }

    @Test
    void crear_reemplazandoUnProtocoloAnulado_loVincula() {
        Protocolo anulado = existente(EstadoProtocolo.ANULADO, 8L);
        anulado.setId(50L);
        when(protocoloRepository.findById(50L)).thenReturn(Optional.of(anulado));

        servicio.crear(8L, solicitud(null, null, 50L));

        ArgumentCaptor<Protocolo> captor = ArgumentCaptor.forClass(Protocolo.class);
        verify(protocoloRepository).save(captor.capture());
        assertSame(anulado, captor.getValue().getProtocoloReemplazado());
    }

    @Test
    void crear_reemplazandoProtocoloInvalido_lanza400() {
        when(protocoloRepository.findById(51L)).thenReturn(Optional.empty());
        Protocolo vigente = existente(EstadoProtocolo.BORRADOR, 8L);
        when(protocoloRepository.findById(52L)).thenReturn(Optional.of(vigente));
        Protocolo deOtroEquipo = existente(EstadoProtocolo.ANULADO, 8L);
        deOtroEquipo.setEquipoId(77L);
        when(protocoloRepository.findById(53L)).thenReturn(Optional.of(deOtroEquipo));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(8L, solicitud(null, null, 51L)));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(8L, solicitud(null, null, 52L)));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(8L, solicitud(null, null, 53L)));
    }

    @Test
    void actualizar_conDatosValidos_modificaElProtocolo() {
        Protocolo protocolo = existente(EstadoProtocolo.BORRADOR, 8L);
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(protocolo));
        LocalDateTime nueva = AHORA.minusHours(2);

        servicio.actualizar(8L, 1L, new ProtocoloActualizacionRequest(nueva, new BigDecimal("18"),
                new BigDecimal("60"), null, "  nota "));

        assertEquals(nueva, protocolo.getFechaHoraPrueba());
        assertEquals(new BigDecimal("60"), protocolo.getHumedadPct());
        assertEquals("nota", protocolo.getObservaciones());
    }

    @Test
    void actualizar_sinFechaConservaLaAnterior() {
        Protocolo protocolo = existente(EstadoProtocolo.BORRADOR, 8L);
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(protocolo));

        servicio.actualizar(8L, 1L, new ProtocoloActualizacionRequest(null, null, null, null, null));

        assertEquals(AHORA, protocolo.getFechaHoraPrueba());
    }

    @Test
    void actualizar_protocoloFormalizado_lanza409() {
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(existente(EstadoProtocolo.FORMALIZADO, 8L)));

        assertEstado(HttpStatus.CONFLICT, () -> servicio.actualizar(8L, 1L,
                new ProtocoloActualizacionRequest(null, null, null, null, null)));
    }

    @Test
    void actualizar_protocoloInexistente_lanza404() {
        when(protocoloRepository.findById(1L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.actualizar(8L, 1L,
                new ProtocoloActualizacionRequest(null, null, null, null, null)));
    }

    @Test
    void registrarEnTerreno_sinHumedad_lanza400() {
        Protocolo protocolo = existente(EstadoProtocolo.BORRADOR, 8L);
        protocolo.setHumedadPct(null);
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(protocolo));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrarEnTerreno(8L, 1L));
    }

    @Test
    void registrarEnTerreno_sinTemperatura_lanza400() {
        Protocolo protocolo = existente(EstadoProtocolo.BORRADOR, 8L);
        protocolo.setTemperaturaC(null);
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(protocolo));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrarEnTerreno(8L, 1L));
    }

    @Test
    void registrarEnTerreno_conEquipoInexistente_lanza400() {
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(existente(EstadoProtocolo.BORRADOR, 8L)));
        when(equiposCliente.buscar(5L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrarEnTerreno(8L, 1L));
    }

    private void pruebasEsperadas(String... codigosRegistrados) {
        TipoPrueba aislacion = TipoPrueba.builder().id(1L).codigo("AISLACION").nombre("Aislación").build();
        TipoPrueba ttr = TipoPrueba.builder().id(2L).codigo("TTR").nombre("TTR").build();
        when(tipoEquipoPruebaRepository.findByTipoEquipoId(1L)).thenReturn(List.of(
                TipoEquipoPrueba.builder().tipoEquipoId(1L).tipoPrueba(aislacion).build(),
                TipoEquipoPrueba.builder().tipoEquipoId(1L).tipoPrueba(ttr).build()));
        List<Prueba> registradas = new java.util.ArrayList<>();
        for (String codigo : codigosRegistrados) {
            registradas.add(Prueba.builder().tipoPrueba("AISLACION".equals(codigo) ? aislacion : ttr).build());
        }
        when(pruebaRepository.findByProtocoloIdOrderByIdAsc(1L)).thenReturn(registradas);
    }

    @Test
    void registrarEnTerreno_conPruebasFaltantes_lanza409() {
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(existente(EstadoProtocolo.BORRADOR, 8L)));
        pruebasEsperadas("AISLACION");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrarEnTerreno(8L, 1L));

        assertEquals(HttpStatus.CONFLICT.value(), error.getStatusCode().value());
        assertEquals(true, error.getReason().contains("TTR"));
    }

    @Test
    void registrarEnTerreno_conTodasLasPruebas_cambiaDeEstadoYRegistraHistorial() {
        Protocolo protocolo = existente(EstadoProtocolo.BORRADOR, 8L);
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(protocolo));
        pruebasEsperadas("AISLACION", "TTR");

        servicio.registrarEnTerreno(8L, 1L);

        assertEquals(EstadoProtocolo.REGISTRADO_TERRENO, protocolo.getEstado());
        ArgumentCaptor<HistorialEstado> historial = ArgumentCaptor.forClass(HistorialEstado.class);
        verify(historialRepository).save(historial.capture());
        assertEquals(EstadoProtocolo.BORRADOR, historial.getValue().getEstadoAnterior());
        assertEquals(EstadoProtocolo.REGISTRADO_TERRENO, historial.getValue().getEstadoNuevo());
    }

    @Test
    void observar_conMotivo_pasaAObservadoYGuardaElMotivo() {
        Protocolo protocolo = existente(EstadoProtocolo.REGISTRADO_TERRENO, 8L);
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(protocolo));

        servicio.observar(9L, 1L, new MotivoRequest(" Falta foto "));

        assertEquals(EstadoProtocolo.OBSERVADO, protocolo.getEstado());
        ArgumentCaptor<HistorialEstado> historial = ArgumentCaptor.forClass(HistorialEstado.class);
        verify(historialRepository).save(historial.capture());
        assertEquals("Falta foto", historial.getValue().getMotivo());
    }

    @Test
    void observar_sinMotivo_lanza400() {
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(existente(EstadoProtocolo.REGISTRADO_TERRENO, 8L)));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.observar(9L, 1L, new MotivoRequest("  ")));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.observar(9L, 1L, new MotivoRequest(null)));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.observar(9L, 1L, null));
        verify(historialRepository, never()).save(any());
    }

    @Test
    void observar_desdeUnEstadoIncorrecto_lanza409() {
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(existente(EstadoProtocolo.BORRADOR, 8L)));

        assertEstado(HttpStatus.CONFLICT, () -> servicio.observar(9L, 1L, new MotivoRequest("x")));
    }

    @Test
    void formalizar_desdeRegistrado_pasaAFormalizado() {
        Protocolo protocolo = existente(EstadoProtocolo.REGISTRADO_TERRENO, 8L);
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(protocolo));

        servicio.formalizar(9L, 1L);

        assertEquals(EstadoProtocolo.FORMALIZADO, protocolo.getEstado());
    }

    @Test
    void formalizar_desdeBorrador_lanza409() {
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(existente(EstadoProtocolo.BORRADOR, 8L)));

        assertEstado(HttpStatus.CONFLICT, () -> servicio.formalizar(9L, 1L));
    }

    @Test
    void anular_conMotivo_pasaAAnulado() {
        Protocolo protocolo = existente(EstadoProtocolo.FORMALIZADO, 8L);
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(protocolo));

        servicio.anular(9L, 1L, new MotivoRequest("Equipo equivocado"));

        assertEquals(EstadoProtocolo.ANULADO, protocolo.getEstado());
    }

    @Test
    void anular_protocoloYaAnulado_lanza409() {
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(existente(EstadoProtocolo.ANULADO, 8L)));

        assertEstado(HttpStatus.CONFLICT, () -> servicio.anular(9L, 1L, new MotivoRequest("x")));
    }

    @Test
    void obtener_protocoloInexistente_lanza404() {
        when(protocoloRepository.findById(1L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.obtener(1L));
    }

    @Test
    void obtener_devuelveElProtocoloMapeado() {
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(existente(EstadoProtocolo.BORRADOR, 8L)));

        assertSame(respuesta, servicio.obtener(1L));
    }

    @Test
    void listar_sinFiltros_usaTodosLosProtocolos() {
        when(protocoloRepository.findAllByOrderByIdDesc()).thenReturn(List.of(existente(EstadoProtocolo.BORRADOR, 8L)));

        assertEquals(1, servicio.listar(null, null, null, null, null).size());
        assertEquals(1, servicio.listar("  ", 4L, 5L, EstadoProtocolo.BORRADOR, false).size());
    }

    @Test
    void listar_aplicaFiltrosYBusquedaPorNumero() {
        when(protocoloRepository.findByNumeroContainingIgnoreCaseOrderByIdDesc("0001"))
                .thenReturn(List.of(existente(EstadoProtocolo.BORRADOR, 8L)));

        assertEquals(0, servicio.listar("0001", 99L, null, null, null).size());
        assertEquals(0, servicio.listar("0001", null, 99L, null, null).size());
        assertEquals(0, servicio.listar("0001", null, null, EstadoProtocolo.ANULADO, null).size());
        assertEquals(1, servicio.listar("0001", 4L, 5L, EstadoProtocolo.BORRADOR, null).size());
    }

    @Test
    void listar_conSoloAtrasados_filtraPorElIndicador() {
        when(protocoloRepository.findAllByOrderByIdDesc()).thenReturn(List.of(existente(EstadoProtocolo.BORRADOR, 8L)));

        assertEquals(0, servicio.listar(null, null, null, null, true).size());
        when(mapper.aProtocolo(any(Protocolo.class))).thenReturn(new ProtocoloResponse(1L, "PT", 4L, "OT-1", 5L, 8L,
                AHORA, null, null, null, null, EstadoProtocolo.BORRADOR, null, null, true, List.of(), List.of()));
        assertEquals(1, servicio.listar(null, null, null, null, true).size());
    }
}
