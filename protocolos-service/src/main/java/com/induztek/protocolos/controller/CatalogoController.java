package com.induztek.protocolos.controller;

import com.induztek.protocolos.dto.TipoPruebaResponse;
import com.induztek.protocolos.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-prueba")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping
    public List<TipoPruebaResponse> listar(@RequestParam(required = false) Long tipoEquipoId) {
        return catalogoService.listarTiposPrueba(tipoEquipoId);
    }
}
