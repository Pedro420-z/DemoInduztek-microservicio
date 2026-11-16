package com.induztek.equipos.config;

import com.induztek.equipos.model.Comuna;
import com.induztek.equipos.model.Region;
import com.induztek.equipos.model.TipoEquipo;
import com.induztek.equipos.repository.ComunaRepository;
import com.induztek.equipos.repository.RegionRepository;
import com.induztek.equipos.repository.TipoEquipoRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatosInicialesTest {

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private ComunaRepository comunaRepository;

    @Mock
    private TipoEquipoRepository tipoEquipoRepository;

    @InjectMocks
    private DatosIniciales datosIniciales;

    @Captor
    private ArgumentCaptor<List<Region>> regionesGuardadas;

    @Captor
    private ArgumentCaptor<List<Comuna>> comunasGuardadas;

    @Captor
    private ArgumentCaptor<List<TipoEquipo>> tiposGuardados;

    @Test
    void run_conBaseVacia_cargaRegionesComunasYTiposDeEquipo() {
        when(regionRepository.count()).thenReturn(0L);
        when(tipoEquipoRepository.count()).thenReturn(0L);
        when(regionRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        datosIniciales.run();

        verify(regionRepository).saveAll(regionesGuardadas.capture());
        assertEquals(16, regionesGuardadas.getValue().size());

        verify(comunaRepository).saveAll(comunasGuardadas.capture());
        assertEquals(17, comunasGuardadas.getValue().size());
        assertTrue(comunasGuardadas.getValue().stream()
                .anyMatch(c -> "Lampa".equals(c.getNombre())
                        && "Metropolitana de Santiago".equals(c.getRegion().getNombre())));

        verify(tipoEquipoRepository).saveAll(tiposGuardados.capture());
        assertEquals(5, tiposGuardados.getValue().size());
        assertTrue(tiposGuardados.getValue().stream()
                .anyMatch(t -> "Relé de protección".equals(t.getNombre())
                        && DatosIniciales.SECUNDARIO.equals(t.getClasificacion())));
    }

    @Test
    void run_conDatosExistentes_noCargaNada() {
        when(regionRepository.count()).thenReturn(16L);
        when(tipoEquipoRepository.count()).thenReturn(5L);

        datosIniciales.run();

        verify(regionRepository, never()).saveAll(anyList());
        verify(comunaRepository, never()).saveAll(anyList());
        verify(tipoEquipoRepository, never()).saveAll(anyList());
    }

    @Test
    void run_conRegionesPeroSinTipos_soloCargaLosTipos() {
        when(regionRepository.count()).thenReturn(16L);
        when(tipoEquipoRepository.count()).thenReturn(0L);

        datosIniciales.run();

        verify(regionRepository, never()).saveAll(anyList());
        verify(tipoEquipoRepository).saveAll(tiposGuardados.capture());
        assertEquals(5, tiposGuardados.getValue().size());
    }
}
