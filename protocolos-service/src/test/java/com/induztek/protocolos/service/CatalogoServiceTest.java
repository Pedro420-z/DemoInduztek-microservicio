package com.induztek.protocolos.service;

import com.induztek.protocolos.dto.TipoPruebaResponse;
import com.induztek.protocolos.model.Parametro;
import com.induztek.protocolos.model.TipoEquipoPrueba;
import com.induztek.protocolos.model.TipoPrueba;
import com.induztek.protocolos.repository.ParametroRepository;
import com.induztek.protocolos.repository.TipoEquipoPruebaRepository;
import com.induztek.protocolos.repository.TipoPruebaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock
    private TipoPruebaRepository tipoPruebaRepository;

    @Mock
    private ParametroRepository parametroRepository;

    @Mock
    private TipoEquipoPruebaRepository tipoEquipoPruebaRepository;

    @InjectMocks
    private CatalogoService servicio;

    private final TipoPrueba aislacion = TipoPrueba.builder().id(1L).codigo("AISLACION").nombre("Aislación").build();
    private final TipoPrueba ttr = TipoPrueba.builder().id(2L).codigo("TTR").nombre("Relación de transformación").build();

    private Parametro parametro(Long id, TipoPrueba tipo, String codigo, String unidad) {
        return Parametro.builder().id(id).tipoPrueba(tipo).codigo(codigo).nombre(codigo).unidad(unidad).build();
    }

    @Test
    void listarTiposPrueba_sinFiltro_devuelveTodosConSusParametros() {
        when(tipoPruebaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(aislacion, ttr));
        when(parametroRepository.findByTipoPruebaIdOrderByIdAsc(1L)).thenReturn(List.of(
                parametro(1L, aislacion, "LECTURA_1MIN", "MΩ"), parametro(2L, aislacion, "LECTURA_10MIN", "MΩ")));
        when(parametroRepository.findByTipoPruebaIdOrderByIdAsc(2L)).thenReturn(List.of(
                parametro(3L, ttr, "RELACION_MEDIDA", "-")));

        List<TipoPruebaResponse> respuesta = servicio.listarTiposPrueba(null);

        assertEquals(2, respuesta.size());
        assertEquals(2, respuesta.get(0).parametros().size());
        assertEquals("MΩ", respuesta.get(0).parametros().get(0).unidad());
        assertEquals(1, respuesta.get(1).parametros().size());
    }

    @Test
    void listarTiposPrueba_conTipoDeEquipo_soloDevuelveLasPruebasPermitidas() {
        when(tipoPruebaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(aislacion, ttr));
        when(tipoEquipoPruebaRepository.findByTipoEquipoId(2L)).thenReturn(List.of(
                TipoEquipoPrueba.builder().id(1L).tipoEquipoId(2L).tipoPrueba(aislacion).build()));
        when(parametroRepository.findByTipoPruebaIdOrderByIdAsc(1L)).thenReturn(List.of(
                parametro(1L, aislacion, "LECTURA_1MIN", "MΩ")));

        List<TipoPruebaResponse> respuesta = servicio.listarTiposPrueba(2L);

        assertEquals(1, respuesta.size());
        assertEquals("AISLACION", respuesta.get(0).codigo());
    }
}
