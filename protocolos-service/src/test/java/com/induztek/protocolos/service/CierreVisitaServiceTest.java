package com.induztek.protocolos.service;

import com.induztek.protocolos.dto.VisitaResponse;
import com.induztek.protocolos.model.EstadoProtocolo;
import com.induztek.protocolos.model.Protocolo;
import com.induztek.protocolos.model.TipoVisita;
import com.induztek.protocolos.model.Visita;
import com.induztek.protocolos.repository.ProtocoloRepository;
import com.induztek.protocolos.repository.VisitaRepository;
import com.induztek.protocolos.util.Roles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CierreVisitaServiceTest {

    @Mock
    private VisitaRepository visitaRepository;
    @Mock
    private ProtocoloRepository protocoloRepository;
    @Mock
    private VisitaService visitaService;
    @Mock
    private ActorService actorService;

    private CierreVisitaService servicio;
    private Visita visita;
    private VisitaResponse respuesta;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);
        servicio = new CierreVisitaService(visitaRepository, protocoloRepository, visitaService, actorService, reloj);
        visita = Visita.builder().id(4L).numeroOt("OT-1").instalacionId(2L).tipoVisita(TipoVisita.SOLO_PRUEBAS).build();
        respuesta = new VisitaResponse(4L, "OT-1", 2L, LocalDate.of(2026, 11, 16), TipoVisita.SOLO_PRUEBAS, 10L,
                List.of(), LocalDateTime.of(2026, 11, 16, 12, 0), true, null);
        when(visitaRepository.findById(4L)).thenReturn(Optional.of(visita));
        when(visitaService.obtener(4L)).thenReturn(respuesta);
    }

    private static Protocolo protocolo(String numero, EstadoProtocolo estado) {
        return Protocolo.builder().numero(numero).estado(estado).build();
    }

    private void assertEstado(HttpStatus esperado, Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(esperado.value(), error.getStatusCode().value());
    }

    @Test
    void cerrar_conTodosLosProtocolosRegistradosOAnulados_cierraLaVisita() {
        when(protocoloRepository.findByVisitaIdOrderByIdAsc(4L)).thenReturn(List.of(
                protocolo("PT-1", EstadoProtocolo.REGISTRADO_TERRENO), protocolo("PT-2", EstadoProtocolo.ANULADO),
                protocolo("PT-3", EstadoProtocolo.FORMALIZADO)));

        VisitaResponse resultado = servicio.cerrar(8L, 4L);

        assertSame(respuesta, resultado);
        assertEquals(LocalDateTime.of(2026, 11, 16, 12, 0), visita.getFechaHoraCierre());
        verify(visitaRepository).save(visita);
        verify(actorService).exigirRol(any(), any(), org.mockito.ArgumentMatchers.eq(Roles.TECNICO),
                org.mockito.ArgumentMatchers.eq(Roles.INGENIERO));
    }

    @Test
    void cerrar_conProtocolosPendientes_lanza409ListandoLosNumeros() {
        when(protocoloRepository.findByVisitaIdOrderByIdAsc(4L)).thenReturn(List.of(
                protocolo("PT-1", EstadoProtocolo.BORRADOR), protocolo("PT-2", EstadoProtocolo.OBSERVADO),
                protocolo("PT-3", EstadoProtocolo.REGISTRADO_TERRENO)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> servicio.cerrar(8L, 4L));

        assertEquals(HttpStatus.CONFLICT.value(), error.getStatusCode().value());
        assertEquals(true, error.getReason().contains("PT-1, PT-2"));
        assertNull(visita.getFechaHoraCierre());
        verify(visitaRepository, never()).save(any());
    }

    @Test
    void cerrar_sinProtocolosOConTodosAnulados_lanza409() {
        when(protocoloRepository.findByVisitaIdOrderByIdAsc(4L)).thenReturn(List.of());
        assertEstado(HttpStatus.CONFLICT, () -> servicio.cerrar(8L, 4L));

        when(protocoloRepository.findByVisitaIdOrderByIdAsc(4L)).thenReturn(List.of(
                protocolo("PT-1", EstadoProtocolo.ANULADO)));
        assertEstado(HttpStatus.CONFLICT, () -> servicio.cerrar(8L, 4L));
    }

    @Test
    void cerrar_visitaYaCerrada_lanza409() {
        visita.setFechaHoraCierre(LocalDateTime.of(2026, 11, 15, 9, 0));

        assertEstado(HttpStatus.CONFLICT, () -> servicio.cerrar(8L, 4L));
    }

    @Test
    void cerrar_visitaInexistente_lanza404() {
        when(visitaRepository.findById(4L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.cerrar(8L, 4L));
    }
}
