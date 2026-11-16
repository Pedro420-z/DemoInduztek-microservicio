package com.induztek.protocolos.controller;

import com.induztek.protocolos.dto.RecepcionRequest;
import com.induztek.protocolos.dto.VisitaRequest;
import com.induztek.protocolos.dto.VisitaResponse;
import com.induztek.protocolos.service.VisitaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/visitas")
@RequiredArgsConstructor
public class VisitaController {

    private static final String CABECERA_ACTOR = "X-Usuario-Id";

    private final VisitaService visitaService;

    @GetMapping
    public List<VisitaResponse> listar(@RequestParam(required = false) String buscar,
                                       @RequestParam(required = false) Long instalacionId,
                                       @RequestParam(required = false) Long ingenieroId,
                                       @RequestParam(required = false) Boolean cerrada) {
        return visitaService.listar(buscar, instalacionId, ingenieroId, cerrada);
    }

    @GetMapping("/{id}")
    public VisitaResponse obtener(@PathVariable Long id) {
        return visitaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisitaResponse crear(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                @RequestBody VisitaRequest datos) {
        return visitaService.crear(actorId, datos);
    }

    @PostMapping("/{id}/recepcion")
    @ResponseStatus(HttpStatus.CREATED)
    public VisitaResponse registrarRecepcion(@PathVariable Long id, @RequestBody RecepcionRequest datos) {
        return visitaService.registrarRecepcion(id, datos);
    }
}
