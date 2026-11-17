package com.induztek.reportes.controller;

import com.induztek.reportes.dto.ActaResponse;
import com.induztek.reportes.dto.HistorialEquipoResponse;
import com.induztek.reportes.dto.ProtocoloExterno;
import com.induztek.reportes.dto.ResumenEquipoResponse;
import com.induztek.reportes.service.ReporteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReporteControllerTest {

    @Mock
    private ReporteService reporteService;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ReporteController(reporteService)).build();
    }

    @Test
    void equipos_devuelveElResumenDeLosEquipos() throws Exception {
        when(reporteService.equipos(30L, null)).thenReturn(List.of(
                new ResumenEquipoResponse(null, 2, "PT-2", null, "APROBADO")));

        mvc.perform(get("/api/reportes/equipos").header("X-Usuario-Id", 30))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PT-2")));
    }

    @Test
    void historialEquipo_devuelveElHistorial() throws Exception {
        when(reporteService.historialEquipo(30L, 5L)).thenReturn(
                new HistorialEquipoResponse(null, 1, Map.of("APROBADO", 1L), List.of()));

        mvc.perform(get("/api/reportes/equipos/5/historial").header("X-Usuario-Id", 30))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"APROBADO\":1")));
    }

    @Test
    void historialEquipo_deOtraEmpresa_devuelve404() throws Exception {
        when(reporteService.historialEquipo(30L, 5L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipo no encontrado"));

        mvc.perform(get("/api/reportes/equipos/5/historial").header("X-Usuario-Id", 30))
                .andExpect(status().isNotFound());
    }

    @Test
    void actas_devuelveLasActas() throws Exception {
        when(reporteService.actas(30L, 2L)).thenReturn(List.of(new ActaResponse(null, "Planta Norte", List.of())));

        mvc.perform(get("/api/reportes/actas").header("X-Usuario-Id", 30).param("instalacionId", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Planta Norte")));
    }

    @Test
    void atrasados_devuelveLosProtocolosAtrasados() throws Exception {
        when(reporteService.atrasados(8L)).thenReturn(List.<ProtocoloExterno>of());

        mvc.perform(get("/api/reportes/protocolos-atrasados").header("X-Usuario-Id", 8))
                .andExpect(status().isOk());
    }

    @Test
    void atrasados_paraUnCliente_devuelve403() throws Exception {
        when(reporteService.atrasados(30L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Un cliente no puede consultar este reporte"));

        mvc.perform(get("/api/reportes/protocolos-atrasados").header("X-Usuario-Id", 30))
                .andExpect(status().isForbidden());
    }
}
