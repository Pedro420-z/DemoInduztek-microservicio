package com.induztek.equipos.controller;

import com.induztek.equipos.dto.InstalacionRequest;
import com.induztek.equipos.dto.InstalacionResponse;
import com.induztek.equipos.service.InstalacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/instalaciones")
@RequiredArgsConstructor
public class InstalacionController {

    private final InstalacionService instalacionService;

    @GetMapping
    public List<InstalacionResponse> listar(@RequestParam(required = false) Long clienteId) {
        return instalacionService.listar(clienteId);
    }

    @GetMapping("/{id}")
    public InstalacionResponse obtener(@PathVariable Long id, @RequestParam(required = false) Long clienteId) {
        return instalacionService.obtener(id, clienteId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstalacionResponse crear(@RequestBody InstalacionRequest datos) {
        return instalacionService.crear(datos);
    }

    @PutMapping("/{id}")
    public InstalacionResponse actualizar(@PathVariable Long id, @RequestBody InstalacionRequest datos) {
        return instalacionService.actualizar(id, datos);
    }
}
