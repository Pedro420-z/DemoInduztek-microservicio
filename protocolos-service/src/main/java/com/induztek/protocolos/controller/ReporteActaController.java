package com.induztek.protocolos.controller;

import com.induztek.protocolos.dto.ReporteActaRequest;
import com.induztek.protocolos.dto.ReporteActaResponse;
import com.induztek.protocolos.dto.ResponderReporteRequest;
import com.induztek.protocolos.model.EstadoReporte;
import com.induztek.protocolos.service.ReporteActaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReporteActaController {

    private static final String CABECERA_ACTOR = "X-Usuario-Id";

    private final ReporteActaService reporteService;

    @GetMapping("/reportes-acta")
    public List<ReporteActaResponse> listar(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                            @RequestParam(required = false) Long visitaId,
                                            @RequestParam(required = false) EstadoReporte estado) {
        return reporteService.listar(actorId, visitaId, estado);
    }

    @GetMapping("/reportes-acta/{id}")
    public ReporteActaResponse obtener(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                       @PathVariable Long id) {
        return reporteService.obtener(actorId, id);
    }

    @PostMapping("/visitas/{visitaId}/reportes")
    @ResponseStatus(HttpStatus.CREATED)
    public ReporteActaResponse crear(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                     @PathVariable Long visitaId,
                                     @RequestBody ReporteActaRequest datos) {
        return reporteService.crear(actorId, visitaId, datos);
    }

    @PutMapping("/reportes-acta/{id}")
    public ReporteActaResponse responder(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                         @PathVariable Long id,
                                         @RequestBody ResponderReporteRequest datos) {
        return reporteService.responder(actorId, id, datos);
    }
}
