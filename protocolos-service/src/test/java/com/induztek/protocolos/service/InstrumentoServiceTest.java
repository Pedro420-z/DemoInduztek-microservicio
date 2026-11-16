package com.induztek.protocolos.service;

import com.induztek.protocolos.dto.CalibracionRequest;
import com.induztek.protocolos.dto.CalibracionResponse;
import com.induztek.protocolos.dto.InstrumentoRequest;
import com.induztek.protocolos.dto.InstrumentoResponse;
import com.induztek.protocolos.model.Calibracion;
import com.induztek.protocolos.model.Instrumento;
import com.induztek.protocolos.repository.CalibracionRepository;
import com.induztek.protocolos.repository.InstrumentoRepository;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstrumentoServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 15);

    @Mock
    private InstrumentoRepository instrumentoRepository;

    @Mock
    private CalibracionRepository calibracionRepository;

    @Mock
    private ActorService actorService;

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);

    private InstrumentoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new InstrumentoService(instrumentoRepository, calibracionRepository, actorService, reloj);
    }

    private Instrumento instrumento(Long id) {
        return Instrumento.builder().id(id).nombre("Megóhmetro " + id).marca("Fluke").modelo("1550C")
                .numeroSerie("SN-" + id).build();
    }

    private Calibracion calibracion(Instrumento instrumento, LocalDate desde, LocalDate hasta) {
        return Calibracion.builder().id(1L).instrumento(instrumento).fechaCalibracion(desde).fechaVencimiento(hasta).build();
    }

    private InstrumentoRequest peticion(String nombre, String serie) {
        return new InstrumentoRequest(nombre, " Fluke ", " ", serie);
    }

    private void assertEstado(HttpStatus esperado, Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(esperado.value(), error.getStatusCode().value());
    }

    private void tresInstrumentos() {
        Instrumento vigente = instrumento(1L);
        Instrumento sinCalibrar = instrumento(2L);
        Instrumento vencido = instrumento(3L);
        when(instrumentoRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(vigente, sinCalibrar, vencido));
        when(calibracionRepository.findFirstByInstrumentoIdOrderByFechaVencimientoDesc(1L))
                .thenReturn(Optional.of(calibracion(vigente, LocalDate.of(2026, 1, 10), LocalDate.of(2027, 1, 10))));
        when(calibracionRepository.findFirstByInstrumentoIdOrderByFechaVencimientoDesc(2L)).thenReturn(Optional.empty());
        when(calibracionRepository.findFirstByInstrumentoIdOrderByFechaVencimientoDesc(3L))
                .thenReturn(Optional.of(calibracion(vencido, LocalDate.of(2025, 1, 10), LocalDate.of(2026, 1, 10))));
    }

    @Test
    void listar_sinFiltro_devuelveTodosIndicandoCualesEstanVigentes() {
        tresInstrumentos();

        List<InstrumentoResponse> respuesta = servicio.listar(null);

        assertEquals(3, respuesta.size());
        assertTrue(respuesta.get(0).vigente());
        assertFalse(respuesta.get(1).vigente());
        assertFalse(respuesta.get(2).vigente());
        assertNull(respuesta.get(1).vencimientoCalibracion());
    }

    @Test
    void listar_soloVigentes_omiteLosVencidosYLosSinCalibracion() {
        tresInstrumentos();

        List<InstrumentoResponse> respuesta = servicio.listar(true);

        assertEquals(1, respuesta.size());
        assertEquals(1L, respuesta.get(0).id());
    }

    @Test
    void listar_conSoloVigentesEnFalso_devuelveTodos() {
        tresInstrumentos();

        assertEquals(3, servicio.listar(false).size());
    }

    @Test
    void obtener_conIdExistente_devuelveElInstrumento() {
        when(instrumentoRepository.findById(1L)).thenReturn(Optional.of(instrumento(1L)));

        InstrumentoResponse respuesta = servicio.obtener(1L);

        assertEquals("SN-1", respuesta.numeroSerie());
        assertFalse(respuesta.vigente());
    }

    @Test
    void obtener_conIdInexistente_lanza404() {
        when(instrumentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.obtener(99L));
    }

    @Test
    void crear_comoJefe_guardaElInstrumentoLimpiandoTextos() {
        when(instrumentoRepository.save(any(Instrumento.class))).thenAnswer(i -> i.getArgument(0));

        InstrumentoResponse respuesta = servicio.crear(1L, peticion(" Megóhmetro ", " SN-100 "));

        verify(actorService).exigirJefeTecnico(1L);
        assertEquals("Megóhmetro", respuesta.nombre());
        assertEquals("SN-100", respuesta.numeroSerie());
        assertEquals("Fluke", respuesta.marca());
        assertNull(respuesta.modelo());
    }

    @Test
    void crear_sinPermiso_noGuardaNada() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(actorService).exigirJefeTecnico(2L);

        assertEstado(HttpStatus.FORBIDDEN, () -> servicio.crear(2L, peticion("Megóhmetro", "SN-1")));
        verify(instrumentoRepository, never()).save(any(Instrumento.class));
    }

    @Test
    void crear_sinNombre_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L, peticion("  ", "SN-1")));
    }

    @Test
    void crear_sinNumeroDeSerie_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L, peticion("Megóhmetro", " ")));
    }

    @Test
    void crear_conNumeroDeSerieNulo_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L, peticion("Megóhmetro", null)));
    }

    @Test
    void crear_conNombreNulo_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(1L, peticion(null, "SN-1")));
    }

    @Test
    void crear_conSerieRepetida_lanza409() {
        when(instrumentoRepository.existsByNumeroSerie("SN-1")).thenReturn(true);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.crear(1L, peticion("Megóhmetro", "SN-1")));
    }

    @Test
    void actualizar_conDatosValidos_cambiaElInstrumento() {
        Instrumento existente = instrumento(1L);
        when(instrumentoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(instrumentoRepository.save(existente)).thenReturn(existente);

        InstrumentoResponse respuesta = servicio.actualizar(1L, 1L, peticion(" Nuevo nombre ", " SN-500 "));

        assertEquals("Nuevo nombre", respuesta.nombre());
        assertEquals("SN-500", respuesta.numeroSerie());
    }

    @Test
    void actualizar_conIdInexistente_lanza404() {
        when(instrumentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.actualizar(1L, 99L, peticion("Megóhmetro", "SN-1")));
    }

    @Test
    void actualizar_sinNombre_lanza400() {
        when(instrumentoRepository.findById(1L)).thenReturn(Optional.of(instrumento(1L)));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.actualizar(1L, 1L, peticion("", "SN-1")));
    }

    @Test
    void actualizar_conSerieDeOtroInstrumento_lanza409() {
        when(instrumentoRepository.findById(1L)).thenReturn(Optional.of(instrumento(1L)));
        when(instrumentoRepository.existsByNumeroSerieAndIdNot("SN-2", 1L)).thenReturn(true);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.actualizar(1L, 1L, peticion("Megóhmetro", "SN-2")));
    }

    @Test
    void historial_devuelveLasCalibracionesDeLaMasReciente() {
        Instrumento existente = instrumento(1L);
        when(instrumentoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(calibracionRepository.findByInstrumentoIdOrderByFechaVencimientoDesc(1L)).thenReturn(List.of(
                calibracion(existente, LocalDate.of(2026, 1, 10), LocalDate.of(2027, 1, 10)),
                calibracion(existente, LocalDate.of(2025, 1, 10), LocalDate.of(2026, 1, 10))));

        List<CalibracionResponse> respuesta = servicio.historial(1L);

        assertEquals(2, respuesta.size());
        assertEquals(LocalDate.of(2027, 1, 10), respuesta.get(0).fechaVencimiento());
    }

    @Test
    void historial_conInstrumentoInexistente_lanza404() {
        when(instrumentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.historial(99L));
    }

    @Test
    void registrarCalibracion_comoJefe_guardaLaNuevaCalibracionSinBorrarLasAnteriores() {
        Instrumento existente = instrumento(1L);
        when(instrumentoRepository.findById(1L)).thenReturn(Optional.of(existente));

        servicio.registrarCalibracion(1L, 1L, new CalibracionRequest(LocalDate.of(2026, 11, 1), LocalDate.of(2027, 11, 1)));

        verify(actorService).exigirJefeTecnico(1L);
        ArgumentCaptor<Calibracion> guardada = ArgumentCaptor.forClass(Calibracion.class);
        verify(calibracionRepository).save(guardada.capture());
        assertEquals(LocalDate.of(2027, 11, 1), guardada.getValue().getFechaVencimiento());
        assertEquals(existente, guardada.getValue().getInstrumento());
    }

    @Test
    void registrarCalibracion_conInstrumentoInexistente_lanza404() {
        when(instrumentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.registrarCalibracion(1L, 99L,
                new CalibracionRequest(LocalDate.of(2026, 11, 1), LocalDate.of(2027, 11, 1))));
    }

    @Test
    void registrarCalibracion_sinFechaDeCalibracion_lanza400() {
        when(instrumentoRepository.findById(1L)).thenReturn(Optional.of(instrumento(1L)));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrarCalibracion(1L, 1L,
                new CalibracionRequest(null, LocalDate.of(2027, 11, 1))));
    }

    @Test
    void registrarCalibracion_sinFechaDeVencimiento_lanza400() {
        when(instrumentoRepository.findById(1L)).thenReturn(Optional.of(instrumento(1L)));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrarCalibracion(1L, 1L,
                new CalibracionRequest(LocalDate.of(2026, 11, 1), null)));
    }

    @Test
    void registrarCalibracion_conVencimientoAnterior_lanza400() {
        when(instrumentoRepository.findById(1L)).thenReturn(Optional.of(instrumento(1L)));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrarCalibracion(1L, 1L,
                new CalibracionRequest(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1))));
        verify(calibracionRepository, never()).save(any(Calibracion.class));
    }

    @Test
    void estaVigente_consultaLaCalibracionALaFechaIndicada() {
        when(calibracionRepository
                .existsByInstrumentoIdAndFechaCalibracionLessThanEqualAndFechaVencimientoGreaterThanEqual(1L, HOY, HOY))
                .thenReturn(true);

        assertTrue(servicio.estaVigente(1L, HOY));
        assertFalse(servicio.estaVigente(2L, HOY));
    }
}
