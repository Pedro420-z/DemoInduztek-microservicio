package com.induztek.protocolos.controller;

import com.induztek.protocolos.dto.MotivoRequest;
import com.induztek.protocolos.dto.ProtocoloActualizacionRequest;
import com.induztek.protocolos.dto.ProtocoloRequest;
import com.induztek.protocolos.dto.ProtocoloResponse;
import com.induztek.protocolos.dto.PruebaRequest;
import com.induztek.protocolos.dto.PruebaResponse;
import com.induztek.protocolos.model.EstadoProtocolo;
import com.induztek.protocolos.service.ProtocoloService;
import com.induztek.protocolos.service.PruebaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/protocolos")
@RequiredArgsConstructor
public class ProtocoloController {

    private static final String CABECERA_ACTOR = "X-Usuario-Id";

    private final ProtocoloService protocoloService;
    private final PruebaService pruebaService;

    @GetMapping
    public List<ProtocoloResponse> listar(@RequestParam(required = false) String buscar,
                                          @RequestParam(required = false) Long visitaId,
                                          @RequestParam(required = false) Long equipoId,
                                          @RequestParam(required = false) EstadoProtocolo estado,
                                          @RequestParam(required = false) Boolean atrasados) {
        return protocoloService.listar(buscar, visitaId, equipoId, estado, atrasados);
    }

    @GetMapping("/{id}")
    public ProtocoloResponse obtener(@PathVariable Long id) {
        return protocoloService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProtocoloResponse crear(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                   @RequestBody ProtocoloRequest datos) {
        return protocoloService.crear(actorId, datos);
    }

    @PutMapping("/{id}")
    public ProtocoloResponse actualizar(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                        @PathVariable Long id,
                                        @RequestBody ProtocoloActualizacionRequest datos) {
        return protocoloService.actualizar(actorId, id, datos);
    }

    @PostMapping("/{id}/pruebas")
    @ResponseStatus(HttpStatus.CREATED)
    public PruebaResponse registrarPrueba(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                          @PathVariable Long id,
                                          @RequestBody PruebaRequest datos) {
        return pruebaService.registrar(actorId, id, datos);
    }

    @DeleteMapping("/{id}/pruebas/{pruebaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarPrueba(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                               @PathVariable Long id,
                               @PathVariable Long pruebaId) {
        pruebaService.eliminar(actorId, id, pruebaId);
    }

    @PostMapping("/{id}/registrar")
    public ProtocoloResponse registrarEnTerreno(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                                @PathVariable Long id) {
        return protocoloService.registrarEnTerreno(actorId, id);
    }

    @PostMapping("/{id}/observar")
    public ProtocoloResponse observar(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                      @PathVariable Long id,
                                      @RequestBody MotivoRequest datos) {
        return protocoloService.observar(actorId, id, datos);
    }

    @PostMapping("/{id}/formalizar")
    public ProtocoloResponse formalizar(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                        @PathVariable Long id) {
        return protocoloService.formalizar(actorId, id);
    }

    @PostMapping("/{id}/anular")
    public ProtocoloResponse anular(@RequestHeader(value = CABECERA_ACTOR, required = false) Long actorId,
                                    @PathVariable Long id,
                                    @RequestBody MotivoRequest datos) {
        return protocoloService.anular(actorId, id, datos);
    }
}
