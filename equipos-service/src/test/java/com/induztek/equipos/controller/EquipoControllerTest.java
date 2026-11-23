package com.induztek.equipos.controller;

import com.induztek.equipos.dto.EquipoRequest;
import com.induztek.equipos.dto.EquipoResponse;
import com.induztek.equipos.service.EquipoService;
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

import java.math.BigDecimal;
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
class EquipoControllerTest {

    private static final String CUERPO = "{\"instalacionId\":1,\"tag\":\"TR-01\",\"tipoEquipoId\":1,\"potenciaKva\":1000}";

    @Mock
    private EquipoService equipoService;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new EquipoController(equipoService)).build();
    }

    private EquipoResponse respuesta() {
        return new EquipoResponse(1L, 1L, "Planta Lampa", 5L, "TR-01", 1L, "Transformador", "PRIMARIO",
                "ABB", null, null, 2020, new BigDecimal("1000"), null, null, null, null, null);
    }

    @Test
    void listar_pasaTodosLosFiltrosAlServicio() throws Exception {
        when(equipoService.listar(5L, 1L, 1L, "tr")).thenReturn(List.of(respuesta()));

        mvc.perform(get("/api/equipos")
                        .param("clienteId", "5")
                        .param("instalacionId", "1")
                        .param("tipoEquipoId", "1")
                        .param("buscar", "tr"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("TR-01")));

        verify(equipoService).listar(5L, 1L, 1L, "tr");
    }

    @Test
    void obtener_pasaElClienteAlServicio() throws Exception {
        when(equipoService.obtener(1L, 5L)).thenReturn(respuesta());

        mvc.perform(get("/api/equipos/1").param("clienteId", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Transformador")));
    }

    @Test
    void obtener_deOtraEmpresa_responde404() throws Exception {
        when(equipoService.obtener(1L, 6L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipo no encontrado"));

        mvc.perform(get("/api/equipos/1").param("clienteId", "6"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crear_responde201() throws Exception {
        when(equipoService.crear(any(EquipoRequest.class))).thenReturn(respuesta());

        mvc.perform(post("/api/equipos").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("TR-01")));
    }

    @Test
    void crear_conTagRepetido_responde409() throws Exception {
        when(equipoService.crear(any(EquipoRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un equipo con ese TAG"));

        mvc.perform(post("/api/equipos").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict());
    }

    @Test
    void actualizar_responde200() throws Exception {
        when(equipoService.actualizar(eq(7L), eq(1L), any(EquipoRequest.class))).thenReturn(respuesta());

        mvc.perform(put("/api/equipos/1").header("X-Usuario-Id", 7).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk());
    }
}
