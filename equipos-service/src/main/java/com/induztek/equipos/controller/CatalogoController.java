package com.induztek.equipos.controller;

import com.induztek.equipos.dto.ComunaResponse;
import com.induztek.equipos.dto.RegionResponse;
import com.induztek.equipos.dto.TipoEquipoResponse;
import com.induztek.equipos.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/regiones")
    public List<RegionResponse> regiones() {
        return catalogoService.listarRegiones();
    }

    @GetMapping("/comunas")
    public List<ComunaResponse> comunas(@RequestParam(required = false) Long regionId) {
        return catalogoService.listarComunas(regionId);
    }

    @GetMapping("/tipos-equipo")
    public List<TipoEquipoResponse> tiposEquipo() {
        return catalogoService.listarTiposEquipo();
    }
}
