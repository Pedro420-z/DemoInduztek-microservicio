package com.induztek.equipos.controller;

import com.induztek.equipos.dto.InstalacionRequest;
import com.induztek.equipos.dto.InstalacionResponse;
import com.induztek.equipos.service.InstalacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InstalacionControllerTest {

    private static final String CUERPO = "{\"clienteId\":5,\"nombre\":\"Planta Lampa\",\"comunaId\":10}";

    @Mock
    private InstalacionService instalacionService;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new InstalacionController(instalacionService)).build();
    }

    private InstalacionResponse respuesta() {
        return new InstalacionResponse(1L, 5L, "Planta Lampa", null, 10L, "Lampa", 7L,
                "Metropolitana de Santiago", null);
    }

    @Test
    void listar_pasaElClienteAlServicio() throws Exception {
        when(instalacionService.listar(5L)).thenReturn(List.of(respuesta()));

        mvc.perform(get("/api/instalaciones").param("clienteId", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Planta Lampa")));

        verify(instalacionService).listar(5L);
    }

    @Test
    void obtener_pasaElClienteAlServicio() throws Exception {
        when(instalacionService.obtener(1L, 5L)).thenReturn(respuesta());

        mvc.perform(get("/api/instalaciones/1").param("clienteId", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lampa")));
    }

    @Test
    void obtener_deOtraEmpresa_responde404() throws Exception {
        when(instalacionService.obtener(1L, 6L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Instalación no encontrada"));

        mvc.perform(get("/api/instalaciones/1").param("clienteId", "6"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crear_responde201() throws Exception {
        when(instalacionService.crear(any(InstalacionRequest.class))).thenReturn(respuesta());

        mvc.perform(post("/api/instalaciones").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("Planta Lampa")));
    }

    @Test
    void actualizar_responde200() throws Exception {
        when(instalacionService.actualizar(eq(1L), any(InstalacionRequest.class))).thenReturn(respuesta());

        mvc.perform(put("/api/instalaciones/1").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk());
    }
}
