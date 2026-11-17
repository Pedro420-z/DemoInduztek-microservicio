package com.induztek.protocolos.service;

import com.induztek.protocolos.client.EquiposCliente;
import com.induztek.protocolos.dto.EquipoExterno;
import com.induztek.protocolos.dto.MedicionRequest;
import com.induztek.protocolos.dto.PruebaRequest;
import com.induztek.protocolos.dto.PruebaResponse;
import com.induztek.protocolos.dto.UsuarioExterno;
import com.induztek.protocolos.model.EstadoProtocolo;
import com.induztek.protocolos.model.Instrumento;
import com.induztek.protocolos.model.MomentoPrueba;
import com.induztek.protocolos.model.Parametro;
import com.induztek.protocolos.model.Protocolo;
import com.induztek.protocolos.model.Prueba;
import com.induztek.protocolos.model.ResultadoPrueba;
import com.induztek.protocolos.model.TipoPrueba;
import com.induztek.protocolos.model.TipoVisita;
import com.induztek.protocolos.model.Visita;
import com.induztek.protocolos.repository.InstrumentoRepository;
import com.induztek.protocolos.repository.ParametroRepository;
import com.induztek.protocolos.repository.ProtocoloRepository;
import com.induztek.protocolos.repository.PruebaRepository;
import com.induztek.protocolos.repository.TipoEquipoPruebaRepository;
import com.induztek.protocolos.repository.TipoPruebaRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PruebaServiceTest {

    @Mock
    private ProtocoloRepository protocoloRepository;
    @Mock
    private PruebaRepository pruebaRepository;
    @Mock
    private TipoPruebaRepository tipoPruebaRepository;
    @Mock
    private ParametroRepository parametroRepository;
    @Mock
    private TipoEquipoPruebaRepository tipoEquipoPruebaRepository;
    @Mock
    private InstrumentoRepository instrumentoRepository;
    @Mock
    private InstrumentoService instrumentoService;
    @Mock
    private ActorService actorService;
    @Mock
    private EquiposCliente equiposCliente;
    @Mock
    private ProtocoloMapper mapper;

    private PruebaService servicio;
    private Protocolo protocolo;
    private Visita visita;
    private TipoPrueba aislacion;
    private PruebaResponse respuesta;

    private static EquipoExterno equipo(Long tipoId, BigDecimal maxContactos) {
        return new EquipoExterno(5L, 2L, "Planta", 30L, "TR-01", tipoId, "Transformador", "POTENCIA",
                null, null, null, null, null, null, null, null, maxContactos, null);
    }

    private TipoPrueba tipo(Long id, String codigo, String... parametros) {
        TipoPrueba tipo = TipoPrueba.builder().id(id).codigo(codigo).nombre(codigo).build();
        when(tipoPruebaRepository.findById(id)).thenReturn(Optional.of(tipo));
        when(tipoEquipoPruebaRepository.existsByTipoEquipoIdAndTipoPruebaId(any(), org.mockito.ArgumentMatchers.eq(id)))
                .thenReturn(true);
        long n = 1;
        List<Parametro> lista = new java.util.ArrayList<>();
        for (String codigoParametro : parametros) {
            lista.add(Parametro.builder().id(id * 10 + n++).tipoPrueba(tipo).codigo(codigoParametro)
                    .nombre(codigoParametro).unidad("u").build());
        }
        when(parametroRepository.findByTipoPruebaIdOrderByIdAsc(id)).thenReturn(lista);
        return tipo;
    }

    @BeforeEach
    void preparar() {
        servicio = new PruebaService(protocoloRepository, pruebaRepository, tipoPruebaRepository, parametroRepository,
                tipoEquipoPruebaRepository, instrumentoRepository, instrumentoService, actorService, equiposCliente,
                new ProtocoloPermisos(), mapper);
        visita = Visita.builder().id(4L).tipoVisita(TipoVisita.SOLO_PRUEBAS).build();
        protocolo = Protocolo.builder().id(1L).visita(visita).equipoId(5L).usuarioRegistraId(8L)
                .fechaHoraPrueba(LocalDateTime.of(2026, 11, 12, 10, 0)).estado(EstadoProtocolo.BORRADOR).build();
        respuesta = new PruebaResponse(1L, 1L, "AISLACION", "Aislación", "AT-BT", MomentoPrueba.UNICA, true, null, 3L,
                ResultadoPrueba.APROBADO, null, null, List.of());
        when(actorService.exigirPersonalActivo(8L)).thenReturn(new UsuarioExterno(8L, 1L, Roles.TECNICO, "T", "1-9",
                "N", "a@b.cl", null, true, null, null, null, false));
        when(protocoloRepository.findById(1L)).thenReturn(Optional.of(protocolo));
        when(equiposCliente.buscar(5L)).thenReturn(Optional.of(equipo(1L, new BigDecimal("100"))));
        when(instrumentoRepository.findById(3L)).thenReturn(Optional.of(Instrumento.builder().id(3L).build()));
        when(instrumentoService.estaVigente(3L, LocalDate.of(2026, 11, 12))).thenReturn(true);
        when(pruebaRepository.save(any(Prueba.class))).thenAnswer(i -> i.getArgument(0));
        when(mapper.aPrueba(any(Prueba.class))).thenReturn(respuesta);
        aislacion = tipo(1L, "AISLACION", "TENSION_PRUEBA", "LECTURA_1MIN", "LECTURA_10MIN");
    }

    private static MedicionRequest m(String parametro, String valor) {
        return new MedicionRequest(parametro, valor == null ? null : new BigDecimal(valor));
    }

    private PruebaRequest realizada(Long tipoId, String elemento, MomentoPrueba momento, Long instrumentoId,
                                    List<MedicionRequest> mediciones) {
        return new PruebaRequest(tipoId, elemento, momento, true, null, instrumentoId, null, null, null, mediciones);
    }

    private List<MedicionRequest> medicionesAislacion() {
        return List.of(m("TENSION_PRUEBA", "5"), m("LECTURA_1MIN", "500"), m("LECTURA_10MIN", "1000"));
    }

    private void assertEstado(HttpStatus esperado, Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(esperado.value(), error.getStatusCode().value());
    }

    @Test
    void registrar_aislacionValida_calculaElResultadoYGuardaLasMediciones() {
        PruebaResponse resultado = servicio.registrar(8L, 1L, realizada(1L, " AT-BT ", null, 3L, medicionesAislacion()));

        ArgumentCaptor<Prueba> captor = ArgumentCaptor.forClass(Prueba.class);
        verify(pruebaRepository).save(captor.capture());
        Prueba guardada = captor.getValue();
        assertSame(respuesta, resultado);
        assertEquals("AT-BT", guardada.getElemento());
        assertEquals(MomentoPrueba.UNICA, guardada.getMomento());
        assertEquals(ResultadoPrueba.APROBADO, guardada.getResultado());
        assertEquals(0, new BigDecimal("2").compareTo(guardada.getValorCalculado()));
        assertEquals(3, guardada.getMediciones().size());
    }

    @Test
    void registrar_sinLecturaDeDiezMinutosEnInterruptor_esValido() {
        when(equiposCliente.buscar(5L)).thenReturn(Optional.of(equipo(2L, null)));

        servicio.registrar(8L, 1L, realizada(1L, "F1", null, 3L,
                List.of(m("TENSION_PRUEBA", "5"), m("LECTURA_1MIN", "500"))));

        ArgumentCaptor<Prueba> captor = ArgumentCaptor.forClass(Prueba.class);
        verify(pruebaRepository).save(captor.capture());
        assertEquals(ResultadoPrueba.APROBADO, captor.getValue().getResultado());
        assertNull(captor.getValue().getValorCalculado());
    }

    @Test
    void registrar_sinLecturaDeDiezMinutosEnTransformador_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L, realizada(1L, "AT-BT", null, 3L,
                List.of(m("TENSION_PRUEBA", "5"), m("LECTURA_1MIN", "500")))));
    }

    @Test
    void registrar_ttr_calculaElErrorPorcentual() {
        tipo(2L, "TTR", "POSICION_TAP", "RELACION_TEORICA", "RELACION_MEDIDA");

        servicio.registrar(8L, 1L, realizada(2L, "AT-BT", null, 3L,
                List.of(m("POSICION_TAP", "3"), m("RELACION_TEORICA", "100"), m("RELACION_MEDIDA", "100.2"))));

        ArgumentCaptor<Prueba> captor = ArgumentCaptor.forClass(Prueba.class);
        verify(pruebaRepository).save(captor.capture());
        assertEquals(ResultadoPrueba.APROBADO, captor.getValue().getResultado());
    }

    @Test
    void registrar_contactos_usaElMaximoDelEquipo() {
        when(equiposCliente.buscar(5L)).thenReturn(Optional.of(equipo(2L, new BigDecimal("100"))));
        tipo(4L, "CONTACTOS", "RESISTENCIA_CONTACTO");

        servicio.registrar(8L, 1L, realizada(4L, "F1", null, 3L, List.of(m("RESISTENCIA_CONTACTO", "130"))));

        ArgumentCaptor<Prueba> captor = ArgumentCaptor.forClass(Prueba.class);
        verify(pruebaRepository).save(captor.capture());
        assertEquals(ResultadoPrueba.RECHAZADO, captor.getValue().getResultado());
    }

    @Test
    void registrar_contactosSinMaximoRegistrado_lanza400() {
        when(equiposCliente.buscar(5L)).thenReturn(Optional.of(equipo(2L, null)));
        tipo(4L, "CONTACTOS", "RESISTENCIA_CONTACTO");

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(4L, "F1", null, 3L, List.of(m("RESISTENCIA_CONTACTO", "130")))));
    }

    @Test
    void registrar_releQueNoOpera_rechazaSinExigirElTiempoMedido() {
        tipo(5L, "OPERACION_RELE", "CORRIENTE_AJUSTE", "CORRIENTE_OPERACION", "TIEMPO_ESPERADO", "TIEMPO_MEDIDO");
        PruebaRequest datos = new PruebaRequest(5L, "R1", null, true, null, 3L, true, null, null,
                List.of(m("CORRIENTE_AJUSTE", "5"), m("CORRIENTE_OPERACION", "5"), m("TIEMPO_ESPERADO", "100")));

        servicio.registrar(8L, 1L, datos);

        ArgumentCaptor<Prueba> captor = ArgumentCaptor.forClass(Prueba.class);
        verify(pruebaRepository).save(captor.capture());
        assertEquals(ResultadoPrueba.RECHAZADO, captor.getValue().getResultado());
    }

    @Test
    void registrar_noOperaEnOtraPrueba_lanza400() {
        PruebaRequest datos = new PruebaRequest(1L, "AT-BT", null, true, null, 3L, true, null, null, medicionesAislacion());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L, datos));
    }

    @Test
    void registrar_devanados_exigeElResultadoManual() {
        tipo(3L, "DEVANADOS", "RESISTENCIA");
        PruebaRequest sinResultado = realizada(3L, "AT", null, 3L, List.of(m("RESISTENCIA", "12")));
        PruebaRequest conResultado = new PruebaRequest(3L, "AT", null, true, null, 3L, null,
                ResultadoPrueba.OBSERVADO, "Dispersión", List.of(m("RESISTENCIA", "12")));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L, sinResultado));
        servicio.registrar(8L, 1L, conResultado);

        ArgumentCaptor<Prueba> captor = ArgumentCaptor.forClass(Prueba.class);
        verify(pruebaRepository).save(captor.capture());
        assertEquals(ResultadoPrueba.OBSERVADO, captor.getValue().getResultado());
        assertEquals("Dispersión", captor.getValue().getComentario());
    }

    @Test
    void registrar_pruebaNoRealizadaConMotivo_noExigeInstrumentoNiMediciones() {
        PruebaRequest datos = new PruebaRequest(1L, "AT-BT", null, false, "  Equipo energizado ", null, null, null,
                null, null);

        servicio.registrar(8L, 1L, datos);

        ArgumentCaptor<Prueba> captor = ArgumentCaptor.forClass(Prueba.class);
        verify(pruebaRepository).save(captor.capture());
        assertEquals("Equipo energizado", captor.getValue().getMotivoNoRealizada());
        assertNull(captor.getValue().getResultado());
    }

    @Test
    void registrar_pruebaNoRealizadaSinMotivo_lanza400() {
        PruebaRequest datos = new PruebaRequest(1L, "AT-BT", null, false, " ", null, null, null, null, null);

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L, datos));
    }

    @Test
    void registrar_sinIndicarSiSeRealizo_lanza400() {
        PruebaRequest datos = new PruebaRequest(1L, "AT-BT", null, null, null, 3L, null, null, null, medicionesAislacion());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L, datos));
    }

    @Test
    void registrar_conInstrumentoVencido_lanza400() {
        when(instrumentoService.estaVigente(3L, LocalDate.of(2026, 11, 12))).thenReturn(false);

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(1L, "AT-BT", null, 3L, medicionesAislacion())));
    }

    @Test
    void registrar_sinInstrumentoOConInstrumentoInexistente_lanza400() {
        when(instrumentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(1L, "AT-BT", null, null, medicionesAislacion())));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(1L, "AT-BT", null, 99L, medicionesAislacion())));
    }

    @Test
    void registrar_conTipoDePruebaInvalido_lanza400() {
        when(tipoPruebaRepository.findById(77L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(null, "AT-BT", null, 3L, medicionesAislacion())));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(77L, "AT-BT", null, 3L, medicionesAislacion())));
    }

    @Test
    void registrar_pruebaQueNoCorrespondeAlTipoDeEquipo_lanza400() {
        when(tipoEquipoPruebaRepository.existsByTipoEquipoIdAndTipoPruebaId(1L, 1L)).thenReturn(false);

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(1L, "AT-BT", null, 3L, medicionesAislacion())));
    }

    @Test
    void registrar_conEquipoInexistente_lanza400() {
        when(equiposCliente.buscar(5L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(1L, "AT-BT", null, 3L, medicionesAislacion())));
    }

    @Test
    void registrar_elementoVacioOMuyLargo_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(1L, " ", null, 3L, medicionesAislacion())));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(1L, "x".repeat(31), null, 3L, medicionesAislacion())));
    }

    @Test
    void registrar_momentoPreviaEnVisitaSoloDePruebas_lanza400YEnMantencionEsValido() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(1L, "AT-BT", MomentoPrueba.PREVIA, 3L, medicionesAislacion())));

        visita.setTipoVisita(TipoVisita.PRUEBAS_Y_MANT);
        servicio.registrar(8L, 1L, realizada(1L, "AT-BT", MomentoPrueba.PREVIA, 3L, medicionesAislacion()));

        verify(pruebaRepository).save(any(Prueba.class));
    }

    @Test
    void registrar_pruebaDuplicada_lanza409() {
        when(pruebaRepository.existsByProtocoloIdAndTipoPruebaIdAndElementoAndMomento(
                1L, 1L, "AT-BT", MomentoPrueba.UNICA)).thenReturn(true);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.registrar(8L, 1L,
                realizada(1L, "AT-BT", null, 3L, medicionesAislacion())));
    }

    @Test
    void registrar_conMedicionesInvalidas_lanza400() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(1L, "AT-BT", null, 3L, List.of(m("INVENTADO", "5")))));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(1L, "AT-BT", null, 3L, List.of(m(null, "5")))));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(1L, "AT-BT", null, 3L, List.of(m("LECTURA_1MIN", null)))));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(1L, "AT-BT", null, 3L, List.of(m("LECTURA_1MIN", "0")))));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(1L, "AT-BT", null, 3L, List.of(m("LECTURA_1MIN", "5"), m("LECTURA_1MIN", "6")))));
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.registrar(8L, 1L,
                realizada(1L, "AT-BT", null, 3L, null)));
    }

    @Test
    void registrar_protocoloInexistente_lanza404() {
        when(protocoloRepository.findById(1L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.registrar(8L, 1L,
                realizada(1L, "AT-BT", null, 3L, medicionesAislacion())));
    }

    @Test
    void registrar_protocoloRegistrado_lanza409() {
        protocolo.setEstado(EstadoProtocolo.REGISTRADO_TERRENO);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.registrar(8L, 1L,
                realizada(1L, "AT-BT", null, 3L, medicionesAislacion())));
    }

    @Test
    void eliminar_pruebaDelProtocolo_laBorra() {
        Prueba prueba = Prueba.builder().id(7L).protocolo(protocolo).build();
        when(pruebaRepository.findById(7L)).thenReturn(Optional.of(prueba));

        servicio.eliminar(8L, 1L, 7L);

        verify(pruebaRepository).delete(prueba);
    }

    @Test
    void eliminar_pruebaDeOtroProtocoloOInexistente_lanza404() {
        Protocolo otro = Protocolo.builder().id(2L).build();
        when(pruebaRepository.findById(7L)).thenReturn(Optional.of(Prueba.builder().id(7L).protocolo(otro).build()));
        when(pruebaRepository.findById(8L)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.eliminar(8L, 1L, 7L));
        assertEstado(HttpStatus.NOT_FOUND, () -> servicio.eliminar(8L, 1L, 8L));
    }
}
