package com.induztek.protocolos.service;

import com.induztek.protocolos.dto.ProtocoloResponse;
import com.induztek.protocolos.model.EstadoProtocolo;
import com.induztek.protocolos.model.HistorialEstado;
import com.induztek.protocolos.model.Medicion;
import com.induztek.protocolos.model.MomentoPrueba;
import com.induztek.protocolos.model.Parametro;
import com.induztek.protocolos.model.Protocolo;
import com.induztek.protocolos.model.Prueba;
import com.induztek.protocolos.model.ResultadoPrueba;
import com.induztek.protocolos.model.TipoPrueba;
import com.induztek.protocolos.model.Visita;
import com.induztek.protocolos.repository.HistorialEstadoRepository;
import com.induztek.protocolos.repository.PruebaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProtocoloMapperTest {

    private static final LocalDateTime PRUEBA = LocalDateTime.of(2026, 11, 11, 10, 0);

    @Mock
    private PruebaRepository pruebaRepository;

    @Mock
    private HistorialEstadoRepository historialRepository;

    private ProtocoloMapper mapper;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);
        mapper = new ProtocoloMapper(pruebaRepository, historialRepository, reloj);
    }

    private Protocolo protocolo(EstadoProtocolo estado, Protocolo reemplazado) {
        Visita visita = Visita.builder().id(4L).numeroOt("OT-1").build();
        return Protocolo.builder().id(1L).numero("PT-2026-0001").visita(visita).equipoId(5L).usuarioRegistraId(8L)
                .fechaHoraPrueba(PRUEBA).estado(estado).protocoloReemplazado(reemplazado).build();
    }

    private Prueba prueba(boolean realizada, ResultadoPrueba resultado) {
        TipoPrueba tipo = TipoPrueba.builder().id(1L).codigo("AISLACION").nombre("Aislación").build();
        Parametro parametro = Parametro.builder().id(1L).codigo("LECTURA_1MIN").nombre("Lectura 1 min").unidad("MΩ").build();
        Prueba prueba = Prueba.builder().id(2L).tipoPrueba(tipo).elemento("AT-BT").momento(MomentoPrueba.UNICA)
                .realizada(realizada).resultado(resultado).build();
        prueba.getMediciones().add(Medicion.builder().prueba(prueba).parametro(parametro).valor(BigDecimal.TEN).build());
        return prueba;
    }

    private HistorialEstado historial(EstadoProtocolo nuevo, LocalDateTime fecha) {
        return HistorialEstado.builder().id(1L).estadoNuevo(nuevo).usuarioId(8L).fechaHora(fecha).build();
    }

    @Test
    void aProtocolo_calculaElResultadoGlobalIgnorandoLasPruebasNoRealizadas() {
        Protocolo protocolo = protocolo(EstadoProtocolo.REGISTRADO_TERRENO, null);
        when(pruebaRepository.findByProtocoloIdOrderByIdAsc(1L)).thenReturn(List.of(
                prueba(true, ResultadoPrueba.APROBADO), prueba(true, ResultadoPrueba.OBSERVADO),
                prueba(false, null)));
        when(historialRepository.findByProtocoloIdOrderByIdAsc(1L)).thenReturn(List.of());

        ProtocoloResponse respuesta = mapper.aProtocolo(protocolo);

        assertEquals(ResultadoPrueba.OBSERVADO, respuesta.resultadoGlobal());
        assertEquals(3, respuesta.pruebas().size());
        assertEquals("OT-1", respuesta.visitaNumeroOt());
        assertEquals("MΩ", respuesta.pruebas().get(0).mediciones().get(0).unidad());
        assertNull(respuesta.protocoloReemplazadoId());
    }

    @Test
    void aProtocolo_indicaElProtocoloReemplazado() {
        Protocolo anulado = protocolo(EstadoProtocolo.ANULADO, null);
        anulado.setId(99L);
        Protocolo protocolo = protocolo(EstadoProtocolo.BORRADOR, anulado);
        when(pruebaRepository.findByProtocoloIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(historialRepository.findByProtocoloIdOrderByIdAsc(1L)).thenReturn(List.of());

        ProtocoloResponse respuesta = mapper.aProtocolo(protocolo);

        assertEquals(99L, respuesta.protocoloReemplazadoId());
        assertNull(respuesta.resultadoGlobal());
    }

    @Test
    void aProtocolo_sinFormalizarYPasadosLosDosDiasHabiles_estaAtrasado() {
        when(pruebaRepository.findByProtocoloIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(historialRepository.findByProtocoloIdOrderByIdAsc(1L)).thenReturn(List.of());

        assertTrue(mapper.aProtocolo(protocolo(EstadoProtocolo.REGISTRADO_TERRENO, null)).atrasado());
    }

    @Test
    void aProtocolo_dentroDelPlazo_noEstaAtrasado() {
        Protocolo protocolo = protocolo(EstadoProtocolo.BORRADOR, null);
        protocolo.setFechaHoraPrueba(LocalDateTime.of(2026, 11, 16, 8, 0));
        when(pruebaRepository.findByProtocoloIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(historialRepository.findByProtocoloIdOrderByIdAsc(1L)).thenReturn(List.of());

        assertFalse(mapper.aProtocolo(protocolo).atrasado());
    }

    @Test
    void aProtocolo_anuladoNuncaEstaAtrasado() {
        when(pruebaRepository.findByProtocoloIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(historialRepository.findByProtocoloIdOrderByIdAsc(1L)).thenReturn(List.of());

        assertFalse(mapper.aProtocolo(protocolo(EstadoProtocolo.ANULADO, null)).atrasado());
    }

    @Test
    void aProtocolo_formalizadoFueraDePlazo_quedaMarcadoComoAtrasado() {
        when(pruebaRepository.findByProtocoloIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(historialRepository.findByProtocoloIdOrderByIdAsc(1L)).thenReturn(List.of(
                historial(EstadoProtocolo.FORMALIZADO, LocalDateTime.of(2026, 11, 16, 9, 0))));

        assertTrue(mapper.aProtocolo(protocolo(EstadoProtocolo.FORMALIZADO, null)).atrasado());
    }

    @Test
    void aProtocolo_formalizadoATiempo_noEstaAtrasado() {
        when(pruebaRepository.findByProtocoloIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(historialRepository.findByProtocoloIdOrderByIdAsc(1L)).thenReturn(List.of(
                historial(EstadoProtocolo.REGISTRADO_TERRENO, LocalDateTime.of(2026, 11, 20, 9, 0)),
                historial(EstadoProtocolo.FORMALIZADO, LocalDateTime.of(2026, 11, 13, 9, 0))));

        assertFalse(mapper.aProtocolo(protocolo(EstadoProtocolo.FORMALIZADO, null)).atrasado());
    }
}
