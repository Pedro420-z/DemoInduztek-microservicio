package com.induztek.protocolos.controller;

import com.induztek.protocolos.dto.CalibracionRequest;
import com.induztek.protocolos.dto.CalibracionResponse;
import com.induztek.protocolos.dto.InstrumentoRequest;
import com.induztek.protocolos.dto.InstrumentoResponse;
import com.induztek.protocolos.service.InstrumentoService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InstrumentoControllerTest {

    private static final String CUERPO = "{\"nombre\":\"Megóhmetro\",\"marca\":\"Fluke\",\"numeroSerie\":\"SN-1\"}";

    @Mock
    private InstrumentoService instrumentoService;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new InstrumentoController(instrumentoService)).build();
    }

    private InstrumentoResponse respuesta() {
        return new InstrumentoResponse(1L, "Megóhmetro", "Fluke", null, "SN-1", null, null, false);
    }

    @Test
    void listar_pasaElFiltroDeVigentesAlServicio() throws Exception {
        when(instrumentoService.listar(true)).thenReturn(List.of(respuesta()));

        mvc.perform(get("/api/instrumentos").param("soloVigentes", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Megóhmetro")));

        verify(instrumentoService).listar(true);
    }

    @Test
    void obtener_responde200() throws Exception {
        when(instrumentoService.obtener(1L)).thenReturn(respuesta());

        mvc.perform(get("/api/instrumentos/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SN-1")));
    }

    @Test
    void crear_leeElActorDeLaCabeceraYResponde201() throws Exception {
        when(instrumentoService.crear(eq(7L), any(InstrumentoRequest.class))).thenReturn(respuesta());

        mvc.perform(post("/api/instrumentos")
                        .header("X-Usuario-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isCreated());

        verify(instrumentoService).crear(eq(7L), any(InstrumentoRequest.class));
    }

    @Test
    void crear_sinPermiso_responde403() throws Exception {
        when(instrumentoService.crear(eq(2L), any(InstrumentoRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el Jefe técnico"));

        mvc.perform(post("/api/instrumentos")
                        .header("X-Usuario-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizar_responde200() throws Exception {
        when(instrumentoService.actualizar(eq(7L), eq(1L), any(InstrumentoRequest.class))).thenReturn(respuesta());

        mvc.perform(put("/api/instrumentos/1")
                        .header("X-Usuario-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO))
                .andExpect(status().isOk());
    }

    @Test
    void historial_responde200() throws Exception {
        when(instrumentoService.historial(1L)).thenReturn(List.of(
                new CalibracionResponse(1L, LocalDate.of(2026, 1, 10), LocalDate.of(2027, 1, 10))));

        mvc.perform(get("/api/instrumentos/1/calibraciones"))
                .andExpect(status().isOk());
    }

    @Test
    void registrarCalibracion_responde201() throws Exception {
        when(instrumentoService.registrarCalibracion(eq(7L), eq(1L), any(CalibracionRequest.class))).thenReturn(respuesta());

        mvc.perform(post("/api/instrumentos/1/calibraciones")
                        .header("X-Usuario-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaCalibracion\":\"2026-11-01\",\"fechaVencimiento\":\"2027-11-01\"}"))
                .andExpect(status().isCreated());
    }
}
