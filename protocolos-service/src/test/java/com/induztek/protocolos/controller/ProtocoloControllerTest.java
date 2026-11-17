package com.induztek.protocolos.controller;

import com.induztek.protocolos.dto.ProtocoloResponse;
import com.induztek.protocolos.dto.PruebaResponse;
import com.induztek.protocolos.model.EstadoProtocolo;
import com.induztek.protocolos.model.MomentoPrueba;
import com.induztek.protocolos.service.ProtocoloService;
import com.induztek.protocolos.service.PruebaService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProtocoloControllerTest {

    @Mock
    private ProtocoloService protocoloService;
    @Mock
    private PruebaService pruebaService;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ProtocoloController(protocoloService, pruebaService)).build();
    }

    private ProtocoloResponse respuesta() {
        return new ProtocoloResponse(1L, "PT-2026-0001", 4L, "OT-1", 5L, 8L, LocalDateTime.of(2026, 11, 16, 10, 0),
                null, null, null, null, EstadoProtocolo.BORRADOR, null, null, false, List.of(), List.of());
    }

    @Test
    void listar_devuelveLosProtocolos() throws Exception {
        when(protocoloService.listar(null, 4L, null, EstadoProtocolo.BORRADOR, null)).thenReturn(List.of(respuesta()));

        mvc.perform(get("/api/protocolos").param("visitaId", "4").param("estado", "BORRADOR"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PT-2026-0001")));
    }

    @Test
    void obtener_devuelveElProtocolo() throws Exception {
        when(protocoloService.obtener(1L)).thenReturn(respuesta());

        mvc.perform(get("/api/protocolos/1")).andExpect(status().isOk());
    }

    @Test
    void crear_devuelve201() throws Exception {
        when(protocoloService.crear(eq(8L), any())).thenReturn(respuesta());

        mvc.perform(post("/api/protocolos").header("X-Usuario-Id", 8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitaId\":4,\"equipoId\":5}"))
                .andExpect(status().isCreated());
    }

    @Test
    void crear_siElServicioRechaza_devuelveElCodigo() throws Exception {
        when(protocoloService.crear(any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "La visita ya está cerrada"));

        mvc.perform(post("/api/protocolos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void actualizar_devuelveElProtocolo() throws Exception {
        when(protocoloService.actualizar(eq(8L), eq(1L), any())).thenReturn(respuesta());

        mvc.perform(put("/api/protocolos/1").header("X-Usuario-Id", 8)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"humedadPct\":50}"))
                .andExpect(status().isOk());
    }

    @Test
    void registrarPrueba_devuelve201() throws Exception {
        PruebaResponse prueba = new PruebaResponse(1L, 1L, "AISLACION", "Aislación", "AT-BT", MomentoPrueba.UNICA,
                true, null, 3L, null, null, null, List.of());
        when(pruebaService.registrar(eq(8L), eq(1L), any())).thenReturn(prueba);

        mvc.perform(post("/api/protocolos/1/pruebas").header("X-Usuario-Id", 8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoPruebaId\":1,\"elemento\":\"AT-BT\",\"realizada\":true}"))
                .andExpect(status().isCreated());
    }

    @Test
    void eliminarPrueba_devuelve204() throws Exception {
        mvc.perform(delete("/api/protocolos/1/pruebas/7").header("X-Usuario-Id", 8))
                .andExpect(status().isNoContent());

        verify(pruebaService).eliminar(8L, 1L, 7L);
    }

    @Test
    void cambiosDeEstado_delegaEnElServicio() throws Exception {
        when(protocoloService.registrarEnTerreno(8L, 1L)).thenReturn(respuesta());
        when(protocoloService.observar(eq(9L), eq(1L), any())).thenReturn(respuesta());
        when(protocoloService.formalizar(9L, 1L)).thenReturn(respuesta());
        when(protocoloService.anular(eq(9L), eq(1L), any())).thenReturn(respuesta());

        mvc.perform(post("/api/protocolos/1/registrar").header("X-Usuario-Id", 8)).andExpect(status().isOk());
        mvc.perform(post("/api/protocolos/1/observar").header("X-Usuario-Id", 9)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Falta foto\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/protocolos/1/formalizar").header("X-Usuario-Id", 9)).andExpect(status().isOk());
        mvc.perform(post("/api/protocolos/1/anular").header("X-Usuario-Id", 9)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Error\"}")).andExpect(status().isOk());
    }
}
