package com.induztek.reportes.controller;

import com.induztek.reportes.dto.ActaResponse;
import com.induztek.reportes.dto.HistorialEquipoResponse;
import com.induztek.reportes.dto.ProtocoloExterno;
import com.induztek.reportes.dto.ResumenEquipoResponse;
import com.induztek.reportes.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private static final String CABECERA_ACTOR = "X-Usuario-Id";

    private final ReporteService reporteService;

    @GetMapping("/equipos")
    public List<ResumenEquipoResponse> equipos(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                               @RequestParam(required = false) Long clienteId) {
        return reporteService.equipos(actorId, clienteId);
    }

    @GetMapping("/equipos/{id}/historial")
    public HistorialEquipoResponse historialEquipo(
            @RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId, @PathVariable Long id) {
        return reporteService.historialEquipo(actorId, id);
    }

    @GetMapping("/actas")
    public List<ActaResponse> actas(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                    @RequestParam(required = false) Long instalacionId) {
        return reporteService.actas(actorId, instalacionId);
    }

    @GetMapping("/protocolos-atrasados")
    public List<ProtocoloExterno> atrasados(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId) {
        return reporteService.atrasados(actorId);
    }
}
