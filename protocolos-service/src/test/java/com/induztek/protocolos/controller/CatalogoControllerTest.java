package com.induztek.protocolos.controller;

import com.induztek.protocolos.dto.ParametroResponse;
import com.induztek.protocolos.dto.TipoPruebaResponse;
import com.induztek.protocolos.service.CatalogoService;
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
    void listar_pasaElTipoDeEquipoAlServicio() throws Exception {
        when(catalogoService.listarTiposPrueba(1L)).thenReturn(List.of(new TipoPruebaResponse(1L, "AISLACION", "Aislación",
                List.of(new ParametroResponse(1L, "LECTURA_1MIN", "Lectura a 1 minuto", "MΩ")))));

        mvc.perform(get("/api/tipos-prueba").param("tipoEquipoId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("LECTURA_1MIN")));

        verify(catalogoService).listarTiposPrueba(1L);
    }
}
