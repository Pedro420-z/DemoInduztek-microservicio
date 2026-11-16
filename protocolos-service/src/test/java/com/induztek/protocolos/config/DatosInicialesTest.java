package com.induztek.protocolos.config;

import com.induztek.protocolos.model.Parametro;
import com.induztek.protocolos.model.TipoEquipoPrueba;
import com.induztek.protocolos.model.TipoPrueba;
import com.induztek.protocolos.repository.ParametroRepository;
import com.induztek.protocolos.repository.TipoEquipoPruebaRepository;
import com.induztek.protocolos.repository.TipoPruebaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatosInicialesTest {

    @Mock
    private TipoPruebaRepository tipoPruebaRepository;

    @Mock
    private ParametroRepository parametroRepository;

    @Mock
    private TipoEquipoPruebaRepository tipoEquipoPruebaRepository;

    @InjectMocks
    private DatosIniciales datosIniciales;

    @Captor
    private ArgumentCaptor<List<Parametro>> parametrosGuardados;

    @Captor
    private ArgumentCaptor<List<TipoEquipoPrueba>> aplicacionesGuardadas;

    @Test
    void run_conBaseVacia_cargaTiposDePruebaParametrosYAplicaciones() {
        when(tipoPruebaRepository.count()).thenReturn(0L);
        when(tipoPruebaRepository.save(any(TipoPrueba.class))).thenAnswer(i -> i.getArgument(0));

        datosIniciales.run();

        verify(parametroRepository).saveAll(parametrosGuardados.capture());
        assertEquals(12, parametrosGuardados.getValue().size());
        assertTrue(parametrosGuardados.getValue().stream()
                .anyMatch(p -> "LECTURA_1MIN".equals(p.getCodigo())
                        && "MΩ".equals(p.getUnidad())
                        && "AISLACION".equals(p.getTipoPrueba().getCodigo())));

        verify(tipoEquipoPruebaRepository).saveAll(aplicacionesGuardadas.capture());
        assertEquals(9, aplicacionesGuardadas.getValue().size());
        assertTrue(aplicacionesGuardadas.getValue().stream()
                .anyMatch(a -> a.getTipoEquipoId() == DatosIniciales.RELE
                        && "OPERACION_RELE".equals(a.getTipoPrueba().getCodigo())));
    }

    @Test
    void run_conDatosExistentes_noCargaNada() {
        when(tipoPruebaRepository.count()).thenReturn(5L);

        datosIniciales.run();

        verify(tipoPruebaRepository, never()).save(any(TipoPrueba.class));
        verify(parametroRepository, never()).saveAll(anyList());
        verify(tipoEquipoPruebaRepository, never()).saveAll(anyList());
    }
}
