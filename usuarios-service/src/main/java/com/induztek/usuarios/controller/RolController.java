package com.induztek.usuarios.controller;

import com.induztek.usuarios.dto.RolResponse;
import com.induztek.usuarios.service.RolService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;

    @GetMapping
    public List<RolResponse> listar() {
        return rolService.listar();
    }
}
