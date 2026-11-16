package com.induztek.protocolos.controller;

import com.induztek.protocolos.dto.CalibracionRequest;
import com.induztek.protocolos.dto.CalibracionResponse;
import com.induztek.protocolos.dto.InstrumentoRequest;
import com.induztek.protocolos.dto.InstrumentoResponse;
import com.induztek.protocolos.service.InstrumentoService;
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
@RequestMapping("/api/instrumentos")
@RequiredArgsConstructor
public class InstrumentoController {

    private static final String CABECERA_ACTOR = "X-Usuario-Id";

    private final InstrumentoService instrumentoService;

    @GetMapping
    public List<InstrumentoResponse> listar(@RequestParam(required = false) Boolean soloVigentes) {
        return instrumentoService.listar(soloVigentes);
    }

    @GetMapping("/{id}")
    public InstrumentoResponse obtener(@PathVariable Long id) {
        return instrumentoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstrumentoResponse crear(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                     @RequestBody InstrumentoRequest datos) {
        return instrumentoService.crear(actorId, datos);
    }

    @PutMapping("/{id}")
    public InstrumentoResponse actualizar(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                          @PathVariable Long id,
                                          @RequestBody InstrumentoRequest datos) {
        return instrumentoService.actualizar(actorId, id, datos);
    }

    @GetMapping("/{id}/calibraciones")
    public List<CalibracionResponse> historial(@PathVariable Long id) {
        return instrumentoService.historial(id);
    }

    @PostMapping("/{id}/calibraciones")
    @ResponseStatus(HttpStatus.CREATED)
    public InstrumentoResponse registrarCalibracion(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                                    @PathVariable Long id,
                                                    @RequestBody CalibracionRequest datos) {
        return instrumentoService.registrarCalibracion(actorId, id, datos);
    }
}
