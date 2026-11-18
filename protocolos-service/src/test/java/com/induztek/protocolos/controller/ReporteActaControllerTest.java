package com.induztek.protocolos.controller;

import com.induztek.protocolos.dto.ReporteActaResponse;
import com.induztek.protocolos.model.EstadoReporte;
import com.induztek.protocolos.service.ReporteActaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReporteActaControllerTest {

    @Mock
    private ReporteActaService reporteService;

    private MockMvc mvc;

    private static ReporteActaResponse respuesta(EstadoReporte estado) {
        LocalDateTime t = LocalDateTime.of(2026, 11, 16, 12, 0);
        return new ReporteActaResponse(1L, 4L, "OT-1", null, null, 30L, "texto", estado, null, null, t, t);
    }

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ReporteActaController(reporteService)).build();
    }

    @Test
    void listar_devuelveLosReportes() throws Exception {
        when(reporteService.listar(30L, null, null)).thenReturn(List.of(respuesta(EstadoReporte.ABIERTO)));

        mvc.perform(get("/api/reportes-acta").header("X-Usuario-Id", 30))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"estado\":\"ABIERTO\"")));
    }

    @Test
    void obtener_devuelveUnReporte() throws Exception {
        when(reporteService.obtener(30L, 1L)).thenReturn(respuesta(EstadoReporte.ABIERTO));

        mvc.perform(get("/api/reportes-acta/1").header("X-Usuario-Id", 30)).andExpect(status().isOk());
    }

    @Test
    void crear_responde201() throws Exception {
        when(reporteService.crear(eq(30L), eq(4L), any())).thenReturn(respuesta(EstadoReporte.ABIERTO));

        mvc.perform(post("/api/visitas/4/reportes").header("X-Usuario-Id", 30)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"protocoloId\":7,\"texto\":\"Valor dudoso\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void responder_responde200() throws Exception {
        when(reporteService.responder(eq(9L), eq(1L), any())).thenReturn(respuesta(EstadoReporte.RESUELTO));

        mvc.perform(put("/api/reportes-acta/1").header("X-Usuario-Id", 9)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"RESUELTO\",\"respuesta\":\"Listo\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("RESUELTO")));
    }
}
