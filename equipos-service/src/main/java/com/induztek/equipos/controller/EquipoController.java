package com.induztek.equipos.controller;

import com.induztek.equipos.dto.EquipoRequest;
import com.induztek.equipos.dto.EquipoResponse;
import com.induztek.equipos.service.EquipoService;
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
@RequestMapping("/api/equipos")
@RequiredArgsConstructor
public class EquipoController {

    private final EquipoService equipoService;

    @GetMapping
    public List<EquipoResponse> listar(@RequestParam(required = false) Long clienteId,
                                       @RequestParam(required = false) Long instalacionId,
                                       @RequestParam(required = false) Long tipoEquipoId,
                                       @RequestParam(required = false) String buscar) {
        return equipoService.listar(clienteId, instalacionId, tipoEquipoId, buscar);
    }

    @GetMapping("/{id}")
    public EquipoResponse obtener(@PathVariable Long id, @RequestParam(required = false) Long clienteId) {
        return equipoService.obtener(id, clienteId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoResponse crear(@RequestBody EquipoRequest datos) {
        return equipoService.crear(datos);
    }

    @PutMapping("/{id}")
    public EquipoResponse actualizar(@RequestHeader(value = "X-Usuario-Id", required = false) Long actorId,
                                     @PathVariable Long id, @RequestBody EquipoRequest datos) {
        return equipoService.actualizar(actorId, id, datos);
    }
}
