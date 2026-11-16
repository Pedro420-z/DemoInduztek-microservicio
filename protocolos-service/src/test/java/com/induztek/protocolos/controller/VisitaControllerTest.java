package com.induztek.protocolos.controller;

import com.induztek.protocolos.dto.RecepcionRequest;
import com.induztek.protocolos.dto.VisitaRequest;
import com.induztek.protocolos.dto.VisitaResponse;
import com.induztek.protocolos.model.TipoVisita;
import com.induztek.protocolos.service.VisitaService;
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

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VisitaControllerTest {

    private static final String CUERPO = "{\"numeroOt\":\"OT-2026-1187\",\"instalacionId\":1,\"fecha\":\"2026-11-16\","
            + "\"tipoVisita\":\"SOLO_PRUEBAS\",\"ingenieroId\":10,\"participantesIds\":[20,21]}";

    @Mock
    private VisitaService visitaService;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new VisitaController(visitaService)).build();
    }

    private VisitaResponse respuesta() {
        return new VisitaResponse(1L, "OT-2026-1187", 1L, LocalDate.of(2026, 11, 16), TipoVisita.SOLO_PRUEBAS,
                10L, List.of(20L, 21L), null, false, null);
    }

    @Test
    void listar_pasaTodosLosFiltrosAlServicio() throws Exception {
        when(visitaService.listar("1187", 1L, 10L, false)).thenReturn(List.of(respuesta()));

        mvc.perform(get("/api/visitas")
                        .param("buscar", "1187")
                        .param("instalacionId", "1")
                        .param("ingenieroId", "10")
                        .param("cerrada", "false"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("OT-2026-1187")));

        verify(visitaService).listar("1187", 1L, 10L, false);
    }

    @Test
    void obtener_responde200() throws Exception {
        when(visitaService.obtener(1L)).thenReturn(respuesta());

        mvc.perform(get("/api/visitas/1"))
                .andExpect(status().isOk());
    }

    @Test
    void obtener_conIdInexistente_responde404() throws Exception {
        when(visitaService.obtener(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita no encontrada"));

        mvc.perform(get("/api/visitas/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crear_leeElActorDeLaCabeceraYResponde201() throws Exception {
        when(visitaService.crear(eq(7L), any(VisitaRequest.class))).thenReturn(respuesta());

        mvc.perform(post("/api/visitas")
                        .header("X-Usuario-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("OT-2026-1187")));

        verify(visitaService).crear(eq(7L), any(VisitaRequest.class));
    }

    @Test
    void crear_conOtRepetida_responde409() throws Exception {
        when(visitaService.crear(eq(7L), any(VisitaRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una visita con ese número de OT"));

        mvc.perform(post("/api/visitas")
                        .header("X-Usuario-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isConflict());
    }

    @Test
    void registrarRecepcion_responde201() throws Exception {
        when(visitaService.registrarRecepcion(eq(1L), any(RecepcionRequest.class))).thenReturn(respuesta());

        mvc.perform(post("/api/visitas/1/recepcion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receptorNombre\":\"Marcela Fuentes\",\"receptorRut\":\"14.226.791-8\",\"receptorCargo\":\"Jefa\"}"))
                .andExpect(status().isCreated());
    }
}
