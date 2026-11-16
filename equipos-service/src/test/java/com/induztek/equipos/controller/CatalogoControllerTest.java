package com.induztek.equipos.controller;

import com.induztek.equipos.dto.ComunaResponse;
import com.induztek.equipos.dto.RegionResponse;
import com.induztek.equipos.dto.TipoEquipoResponse;
import com.induztek.equipos.service.CatalogoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CatalogoControllerTest {

    @Mock
    private CatalogoService catalogoService;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new CatalogoController(catalogoService)).build();
    }

    @Test
    void regiones_responde200ConLaLista() throws Exception {
        when(catalogoService.listarRegiones()).thenReturn(List.of(new RegionResponse(7L, "Metropolitana de Santiago")));

        mvc.perform(get("/api/regiones"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Metropolitana de Santiago")));
    }

    @Test
    void comunas_pasaElFiltroDeRegionAlServicio() throws Exception {
        when(catalogoService.listarComunas(7L)).thenReturn(List.of(new ComunaResponse(10L, "Lampa", 7L, "Metropolitana de Santiago")));

        mvc.perform(get("/api/comunas").param("regionId", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lampa")));

        verify(catalogoService).listarComunas(7L);
    }

    @Test
    void tiposEquipo_responde200ConLaLista() throws Exception {
        when(catalogoService.listarTiposEquipo()).thenReturn(List.of(new TipoEquipoResponse(1L, "Transformador", "PRIMARIO")));

        mvc.perform(get("/api/tipos-equipo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Transformador")));
    }
}
