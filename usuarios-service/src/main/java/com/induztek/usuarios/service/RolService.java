package com.induztek.usuarios.service;

import com.induztek.usuarios.dto.RolResponse;
import com.induztek.usuarios.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepository;

    public List<RolResponse> listar() {
        return rolRepository.findAllByOrderByIdAsc().stream()
                .map(rol -> new RolResponse(rol.getId(), rol.getCodigo(), rol.getNombre()))
                .toList();
    }
}
