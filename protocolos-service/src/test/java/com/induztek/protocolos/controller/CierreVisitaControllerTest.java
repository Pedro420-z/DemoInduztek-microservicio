package com.induztek.protocolos.controller;

import com.induztek.protocolos.dto.VisitaResponse;
import com.induztek.protocolos.model.TipoVisita;
import com.induztek.protocolos.service.CierreVisitaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CierreVisitaControllerTest {

    @Mock
    private CierreVisitaService cierreVisitaService;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new CierreVisitaController(cierreVisitaService)).build();
    }

    @Test
    void cerrar_devuelveLaVisitaCerrada() throws Exception {
        when(cierreVisitaService.cerrar(8L, 4L)).thenReturn(new VisitaResponse(4L, "OT-1", 2L,
                LocalDate.of(2026, 11, 16), TipoVisita.SOLO_PRUEBAS, 10L, List.of(),
                LocalDateTime.of(2026, 11, 16, 12, 0), true, null));

        mvc.perform(post("/api/visitas/4/cierre").header("X-Usuario-Id", 8))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"cerrada\":true")));
    }

    @Test
    void cerrar_conProtocolosPendientes_devuelve409() throws Exception {
        when(cierreVisitaService.cerrar(8L, 4L))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "faltan protocolos"));

        mvc.perform(post("/api/visitas/4/cierre").header("X-Usuario-Id", 8)).andExpect(status().isConflict());
    }
}
