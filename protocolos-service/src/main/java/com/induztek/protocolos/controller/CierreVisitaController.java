package com.induztek.protocolos.controller;

import com.induztek.protocolos.dto.VisitaResponse;
import com.induztek.protocolos.service.CierreVisitaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visitas")
@RequiredArgsConstructor
public class CierreVisitaController {

    private static final String CABECERA_ACTOR = "X-Usuario-Id";

    private final CierreVisitaService cierreVisitaService;

    @PostMapping("/{id}/cierre")
    public VisitaResponse cerrar(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                 @PathVariable Long id) {
        return cierreVisitaService.cerrar(actorId, id);
    }
}
