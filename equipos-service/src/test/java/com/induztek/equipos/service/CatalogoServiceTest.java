package com.induztek.equipos.service;

import com.induztek.equipos.dto.ComunaResponse;
import com.induztek.equipos.dto.RegionResponse;
import com.induztek.equipos.dto.TipoEquipoResponse;
import com.induztek.equipos.model.Comuna;
import com.induztek.equipos.model.Region;
import com.induztek.equipos.model.TipoEquipo;
import com.induztek.equipos.repository.ComunaRepository;
import com.induztek.equipos.repository.RegionRepository;
import com.induztek.equipos.repository.TipoEquipoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private ComunaRepository comunaRepository;

    @Mock
    private TipoEquipoRepository tipoEquipoRepository;

    @InjectMocks
    private CatalogoService servicio;

    private final Region region = Region.builder().id(7L).nombre("Metropolitana de Santiago").build();
    private final Comuna comuna = Comuna.builder().id(10L).nombre("Lampa").region(region).build();

    @Test
    void listarRegiones_devuelveLasRegionesOrdenadas() {
        when(regionRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(region));

        List<RegionResponse> respuesta = servicio.listarRegiones();

        assertEquals(1, respuesta.size());
        assertEquals("Metropolitana de Santiago", respuesta.get(0).nombre());
    }

    @Test
    void listarComunas_sinRegion_devuelveTodas() {
        when(comunaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(comuna));

        List<ComunaResponse> respuesta = servicio.listarComunas(null);

        assertEquals("Lampa", respuesta.get(0).nombre());
        assertEquals(7L, respuesta.get(0).regionId());
        verify(comunaRepository, never()).findByRegionIdOrderByNombreAsc(7L);
    }

    @Test
    void listarComunas_conRegion_filtraPorRegion() {
        when(comunaRepository.findByRegionIdOrderByNombreAsc(7L)).thenReturn(List.of(comuna));

        List<ComunaResponse> respuesta = servicio.listarComunas(7L);

        assertEquals(1, respuesta.size());
        assertEquals("Metropolitana de Santiago", respuesta.get(0).regionNombre());
    }

    @Test
    void listarTiposEquipo_devuelveNombreYClasificacion() {
        when(tipoEquipoRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(
                TipoEquipo.builder().id(1L).nombre("Transformador").clasificacion("PRIMARIO").build()));

        List<TipoEquipoResponse> respuesta = servicio.listarTiposEquipo();

        assertEquals("Transformador", respuesta.get(0).nombre());
        assertEquals("PRIMARIO", respuesta.get(0).clasificacion());
    }
}
